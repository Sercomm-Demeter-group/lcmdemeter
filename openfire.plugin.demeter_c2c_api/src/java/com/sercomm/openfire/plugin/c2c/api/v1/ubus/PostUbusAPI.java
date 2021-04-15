package com.sercomm.openfire.plugin.c2c.api.v1.ubus;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.util.XStringUtil;
import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.HeaderField;
import com.sercomm.commons.util.Json;
import com.sercomm.demeter.microservices.client.v1.PostUbusCommandRequest;
import com.sercomm.demeter.microservices.client.v1.PostUbusCommandResult;
import com.sercomm.openfire.plugin.HttpServer;
import com.sercomm.openfire.plugin.UbusManager;
import com.sercomm.openfire.plugin.c2c.exception.InternalErrorException;
import com.sercomm.openfire.plugin.c2c.exception.UMEiException;
import com.sercomm.openfire.plugin.exception.DemeterException;

@Path("umei/v1")
public class PostUbusAPI
{
    private static final Logger log = LoggerFactory.getLogger(PostUbusAPI.class);

	@POST
	@Path("device/{nodeName}/ubus")
	@Produces({MediaType.APPLICATION_JSON})
	public Response execute(
			@HeaderParam(HeaderField.HEADER_REQUEST_ID) String requestId,
            @HeaderParam(HeaderField.HEADER_ORIGINATOR_ID) String originatorId,
            @PathParam("nodeName") String nodeName,
			String requestPayload) 
    throws UMEiException, InternalErrorException
	{
        Response response = null;
        Response.Status status = Status.OK;
        
        String errorMessage = XStringUtil.BLANK;
		try
		{
            if(false == NameRule.isDevice(nodeName))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID NODE NAME: " + nodeName;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(XStringUtil.isBlank(requestPayload))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "REQUEST PAYLOAD WAS BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            final String serial = NameRule.toDeviceSerial(nodeName);
            final String mac = NameRule.toDeviceMac(nodeName);
            
            String method;
            String path;
            String payloadString;

            PostUbusCommandRequest.RequestData requestData;
            try
            {
                BodyPayload bodyPayload = Json.mapper().readValue(
                    requestPayload,
                    BodyPayload.class);
                
                requestData = bodyPayload.getDesire(
                    PostUbusCommandRequest.RequestData.class);
                
                method = requestData.getMethod();
                path = requestData.getPath();
                payloadString = requestData.getPayloadString();
            }
            catch(Throwable ignored)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID REQUEST PAYLOAD: " + requestPayload;

                throw new UMEiException(
                    errorMessage,
                    status);
            }
                        
            if(XStringUtil.isBlank(method) ||
               XStringUtil.isBlank(path))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "REQUIRED ARGUMENT(S) CANNOT BE BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            String resultString;
            try
            {
                resultString = 
                        Json.build(UbusManager.getInstance().fire(
                            serial, 
                            mac, 
                            method, 
                            path, 
                            payloadString, 
                            30 * 1000L));
            }
            catch(DemeterException e)
            {
                status = Response.Status.BAD_GATEWAY;
                errorMessage = e.getMessage();

                throw new UMEiException(
                    errorMessage,
                    status);
            }
                            
            PostUbusCommandResult.ResultData data = new PostUbusCommandResult.ResultData();
            data.setResult(resultString);
            
            BodyPayload bodyPayload = new BodyPayload()
                    .withMeta(null)
                    .withData(data);
            
            response = Response
                    .status(status)
                    .header(HeaderField.HEADER_REQUEST_ID, requestId)
                    .header(HeaderField.HEADER_ORIGINATOR_ID, originatorId)
                    .header(HeaderField.HEADER_RECEIVER_ID, HttpServer.SERVICE_NAME)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(bodyPayload.toString())
                    .build();
		}
        catch(UMEiException e)
        {
            status = e.getErrorStatus();            
            errorMessage = e.getMessage();
            throw e.withIdentifyValues(
                requestId,
                originatorId,
                HttpServer.SERVICE_NAME);
        }
		catch(Throwable t)
		{
            status = Response.Status.INTERNAL_SERVER_ERROR;            
            errorMessage = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t);
            throw new InternalErrorException(t.getMessage()).withIdentifyValues(
                requestId,
                HttpServer.SERVICE_NAME, 
                originatorId);
		}
		
		log.info("({},{},{},{}); {}",
            requestId,
            originatorId,
            nodeName,
            requestPayload,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());
		
        return response;
	}
}
