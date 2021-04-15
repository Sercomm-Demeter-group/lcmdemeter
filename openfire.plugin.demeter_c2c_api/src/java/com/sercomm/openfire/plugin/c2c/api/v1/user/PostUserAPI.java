package com.sercomm.openfire.plugin.c2c.api.v1.user;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.HeaderField;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.demeter.microservices.client.v1.PostUserRequest;
import com.sercomm.openfire.plugin.EndUserManager;
import com.sercomm.openfire.plugin.HttpServer;
import com.sercomm.openfire.plugin.c2c.exception.InternalErrorException;
import com.sercomm.openfire.plugin.c2c.exception.UMEiException;
import com.sercomm.openfire.plugin.define.EndUserRole;

@Path("umei/v1")
public class PostUserAPI
{
    private static final Logger log = LoggerFactory.getLogger(PostUserAPI.class);

    @POST
    @Path("user")
    @Produces({MediaType.APPLICATION_JSON})
    public Response execute(
            @HeaderParam(HeaderField.HEADER_REQUEST_ID) String requestId,
            @HeaderParam(HeaderField.HEADER_ORIGINATOR_ID) String originatorId,
            String requestPayload) 
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        String errorMessage = XStringUtil.BLANK;
        try
        {
            if(XStringUtil.isBlank(requestPayload))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "REQUEST PAYLOAD WAS BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }
            
            String username;
            String password;
            String role;
            try
            {
                BodyPayload bodyPayload = Json.mapper().readValue(
                    requestPayload,
                    BodyPayload.class);
                
                PostUserRequest.RequestData requestData = bodyPayload.getDesire(
                    PostUserRequest.RequestData.class);
                
                username = requestData.getUsername();
                password = requestData.getPassword();
                role = requestData.getRole();
            }
            catch(Throwable ignored)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID REQUEST PAYLOAD: " + requestPayload;

                throw new UMEiException(
                    errorMessage,
                    status);
            }
            
            if(XStringUtil.isBlank(username) ||
               XStringUtil.isBlank(password) ||
               XStringUtil.isBlank(role))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "MANDATORY ARGUMENT(S) CANNOT BE BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }
            
            if(EndUserManager.getInstance().isRegisteredUser(username))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "USER ALREADY EXISTS: " + username;

                throw new UMEiException(
                    errorMessage,
                    status);
            }
            
            EndUserRole endUserRole = EndUserRole.fromString(role);
            if(null == endUserRole)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INVALID ROLE, SHOULD BE 'member' or 'admin'";

                throw new UMEiException(
                    errorMessage,
                    status);
            }
            
            EndUserManager.getInstance().createUser(
                username, 
                password, 
                endUserRole);
            
            BodyPayload bodyPayload = new BodyPayload()
                    .withMeta(null)
                    .withData(null);
            
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

        log.info("({},{},{}); {}",
            requestId,
            originatorId,
            requestPayload,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());
        
        return response;
    }
}
