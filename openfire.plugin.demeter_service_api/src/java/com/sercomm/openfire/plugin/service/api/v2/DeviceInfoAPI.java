package com.sercomm.openfire.plugin.service.api.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.HeaderField;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.demeter.microservices.client.v1.PostUbusCommandResult;
import com.sercomm.openfire.plugin.UbusManager;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path(DeviceInfoAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR})
public class DeviceInfoAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(DeviceInfoAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    public static final String SERVICE_NAME = "demeter.core";

    private static String getInfo(String nodeName, String path)
    throws InternalErrorException
    {
        //return "";
        
        String requestId = "7fe79306-366a-4098-bbb8-9ea5c77c09dc";
        String originatorId = "sercomm.com";
        Response response = null;
        Response.Status status = Status.OK;
        
        String errorMessage = XStringUtil.BLANK;
		try
		{
            
            if(false == NameRule.isDevice(nodeName))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID NODE NAME: " + nodeName;
            }

            final String serial = NameRule.toDeviceSerial(nodeName);
            final String mac = NameRule.toDeviceMac(nodeName);
            
            String method = "Get";
            String payloadString = "";

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
                    .header(HeaderField.HEADER_RECEIVER_ID, SERVICE_NAME)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(bodyPayload.toString())
                    .build();
		}
		catch(Throwable t)
		{
            status = Response.Status.INTERNAL_SERVER_ERROR;            
            errorMessage = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t);
		}
		
		log.info("({},{},{}); {}",
            requestId,
            originatorId,
            nodeName,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());
		
        return response.getEntity().toString();
    }

    @Context
    private HttpServletRequest request;

    @GET
    @Path("devices/{deviceId}/info")
    @Produces({MediaType.APPLICATION_JSON})
    public Response get(@PathParam("deviceId") String deviceId)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        Integer size = 0;
        String errorMessage = XStringUtil.BLANK;
        try
        {

            List<GetDeviceInfoResult> result = null;
            result = new ArrayList<>();
            String res = getInfo(deviceId,"System.Resources");
            if(res != null) {
                JsonNode node = Json.parse(res);
                JsonNode nodeData = node.get("data");
                String ResultText = nodeData.get("result").textValue();;
                JsonNode nodeResult = Json.parse(ResultText);
                JsonNode nodeBody = nodeResult.get("Body");
                Iterator<String> itr = nodeBody.fieldNames();
                while (itr.hasNext()) {
                    String key_field = itr.next();
                    if(key_field == "Uptime") {
                        String value_field = nodeBody.get(key_field).asText();
                        GetDeviceInfoResult object = null;
                        object = new GetDeviceInfoResult();
                        object.setParam(key_field,value_field);
                        result.add(object);
                    }
                }
            }
            res = getInfo(deviceId,"Interfaces.Physical.Network.LAN.Wi-Fi.EasyMesh");
            if(res != null){
                JsonNode node = Json.parse(res);
                JsonNode nodeData = node.get("data");
                String ResultText = nodeData.get("result").textValue();;
                JsonNode nodeResult = Json.parse(ResultText);
                JsonNode nodeBody = nodeResult.get("Body");
                Iterator<String> itr = nodeBody.fieldNames();
                while (itr.hasNext()) {
                    String key_field = itr.next();
                    String value_field = nodeBody.get(key_field).asText();
                    GetDeviceInfoResult object = null;
                    object = new GetDeviceInfoResult();
                    object.setParam(key_field,value_field);
                    result.add(object);
                }
            }

            res = getInfo(deviceId,"Services.Local.HostManager");
            if(res != null) {
                JsonNode node = Json.parse(res);
                JsonNode nodeData = node.get("data");
                String ResultText = nodeData.get("result").textValue();;
                JsonNode nodeResult = Json.parse(ResultText);
                JsonNode nodeBody = nodeResult.get("Body");
                JsonNode nodeStat = nodeBody.get("Statistics");
                JsonNode nodeDevices = nodeStat.get("Devices");
                Iterator<String> itr = nodeDevices.fieldNames();
                while (itr.hasNext()) {
                    String key_field = itr.next();
                    String value_field = nodeDevices.get(key_field).asText();
                    GetDeviceInfoResult object = null;
                    object = new GetDeviceInfoResult();
                    object.setParam(key_field + " clients",value_field);
                    result.add(object);
                }

                JsonNode nodeInterfaces = nodeStat.get("Interfaces");
                JsonNode nodeLAN = nodeInterfaces.get("Interfaces.Physical.Network.LAN.EthernetSwitch");
                itr = nodeLAN.fieldNames();
                while (itr.hasNext()) {
                    String key_field = itr.next();
                    String value_field = nodeLAN.get(key_field).asText();
                    GetDeviceInfoResult object = null;
                    object = new GetDeviceInfoResult();
                    object.setParam(key_field + " LAN clients",value_field);
                    result.add(object);
                }
                JsonNode nodeWiFi = nodeInterfaces.get("Interfaces.Physical.Data.LAN.Wi-Fi");
                itr = nodeWiFi.fieldNames();
                while (itr.hasNext()) {
                    String key_field = itr.next();
                    String value_field = nodeWiFi.get(key_field).asText();
                    GetDeviceInfoResult object = null;
                    object = new GetDeviceInfoResult();
                    object.setParam(key_field + " WiFi clients",value_field);
                    result.add(object);
                }
            }
            BodyPayload bodyPayload = new BodyPayload()
                    .withMeta(null)
                    .withData(result);

            response = Response
                .status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(bodyPayload.toString())
                .build();
        }
        catch(UMEiException e)
        {
            status = e.getErrorStatus();            
            errorMessage = e.getMessage();
            throw e;
        }
        catch(Throwable t)
        {
            status = Response.Status.INTERNAL_SERVER_ERROR;            
            errorMessage = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t);
            throw new InternalErrorException(t.getMessage());
        }

        log.info("({},{},{},{}); {}",
            userId,
            sessionId,
            deviceId,
            size,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    public static class GetDeviceInfoResult
    {
        private String name;
        private String value;

        public String getName()
        {
            return name;
        }

        public String getValue()
        {
            return value;
        }

        public void setParam(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        public void setName(String name)
        {
            this.name = name;
        }
        public void setValue(String value)
        {
            this.value = value;
        }
    }
}
