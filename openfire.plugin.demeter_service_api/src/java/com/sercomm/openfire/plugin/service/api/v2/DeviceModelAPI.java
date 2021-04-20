package com.sercomm.openfire.plugin.service.api.v2;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.DeviceModelManager;
import com.sercomm.openfire.plugin.cache.DeviceModelCache;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(DeviceModelAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR})
public class DeviceModelAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(DeviceModelAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    @Context 
    private HttpServletRequest request;

    @POST
    @Path("model")
    @Produces({MediaType.APPLICATION_JSON})
    public Response post(String requestPayload)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String modelName;

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

            try
            {
                BodyPayload bodyPayload = Json.mapper().readValue(
                    requestPayload,
                    BodyPayload.class);

                PostDeviceModelRequest request = bodyPayload.getDesire(
                    PostDeviceModelRequest.class);

                modelName = request.getModelName();
            }
            catch(Throwable ignored)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID REQUEST PAYLOAD: " + requestPayload;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(XStringUtil.isBlank(modelName))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "MANDATORY PARAMETER(S) CANNOT BE BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            DeviceModelCache deviceModelCache = 
                    DeviceModelManager.getInstance().createDeviceModel(modelName, XStringUtil.BLANK);

            PostDeviceModelResult result = new PostDeviceModelResult();
            result.setModelId(deviceModelCache.getId());

            // response
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

        log.info("({},{},{}); {}",
            userId,
            sessionId,
            modelName,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }
    
    @PUT
    @Path("model/{targetId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response put(
            @PathParam("targetId") String targetId,
            String requestPayload)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String reqStatusString;

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

            try
            {
                BodyPayload bodyPayload = Json.mapper().readValue(
                    requestPayload,
                    BodyPayload.class);

                PutDeviceModelRequest request = bodyPayload.getDesire(
                    PutDeviceModelRequest.class);
                
                reqStatusString = request.getStatus();
            }
            catch(Throwable ignored)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID REQUEST PAYLOAD: " + requestPayload;

                throw new UMEiException(
                    errorMessage,
                    status);
            }
            
            int reqStatus;
            switch(reqStatusString)
            {
                case "enable":
                    reqStatus = 1;
                    break;
                case "disable":
                    reqStatus = 0;
                    break;
                default:
                    status = Response.Status.BAD_REQUEST;
                    errorMessage = "INALID 'status' PARAMETER: " + requestPayload;
                    throw new UMEiException(
                        errorMessage,
                        status);
            }
            
            DeviceModelCache deviceModelCache;
            try
            {
                deviceModelCache = DeviceModelManager.getInstance().getDeviceModelById(targetId);
                
                DeviceModelManager.getInstance().updateDeviceModel(
                    deviceModelCache.getModelName(),
                    reqStatus,
                    deviceModelCache.getScript());
            }
            catch(DemeterException e)
            {
                status = Status.FORBIDDEN;
                errorMessage = e.getMessage();
                throw new UMEiException(
                    errorMessage,
                    status);
            }

            // response
            BodyPayload bodyPayload = new BodyPayload()
                    .withMeta(null)
                    .withData(null);

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
            targetId,
            reqStatusString,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    @DELETE
    @Path("model/{targetId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response delete(
            @PathParam("targetId") String targetId)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String errorMessage = XStringUtil.BLANK;
        try
        {            
            DeviceModelCache deviceModelCache;
            try
            {
                deviceModelCache = DeviceModelManager.getInstance().getDeviceModelById(targetId);                
                DeviceModelManager.getInstance().deleteDeviceModel(deviceModelCache.getModelName());
            }
            catch(DemeterException e)
            {
                status = Status.FORBIDDEN;
                errorMessage = e.getMessage();
                throw new UMEiException(
                    errorMessage,
                    status);
            }

            // response
            BodyPayload bodyPayload = new BodyPayload()
                    .withMeta(null)
                    .withData(null);

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

        log.info("({},{},{}); {}",
            userId,
            sessionId,
            targetId,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    public static class PostDeviceModelRequest
    {
        private String modelName;

        public String getModelName()
        {
            return modelName;
        }

        public void setModelName(String modelName)
        {
            this.modelName = modelName;
        }
    }
    
    public static class PostDeviceModelResult
    {
        private String modelId;

        public String getModelId()
        {
            return modelId;
        }

        public void setModelId(String modelId)
        {
            this.modelId = modelId;
        }
    }
    
    public static class PutDeviceModelRequest
    {
        private String status;

        public String getStatus()
        {
            return status;
        }

        public void setStatus(String status)
        {
            this.status = status;
        }
        
    }
}
