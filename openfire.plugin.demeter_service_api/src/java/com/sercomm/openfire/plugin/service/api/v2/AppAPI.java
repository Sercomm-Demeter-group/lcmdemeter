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
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.DeviceModelManager;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(AppAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR})
public class AppAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(AppAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    @Context 
    private HttpServletRequest request;

    @POST
    @Path("application")
    @Produces({MediaType.APPLICATION_JSON})
    public Response post(String requestPayload)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String name;
        String publisher;
        String model;
        String description;

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

                PostApplicationRequest request = bodyPayload.getDesire(
                    PostApplicationRequest.class);

                name = request.getName();
                publisher = request.getPublisher();
                model = request.getModel();
                description = request.getDescription();
            }
            catch(Throwable ignored)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID REQUEST PAYLOAD: " + requestPayload;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(XStringUtil.isBlank(name) ||
               XStringUtil.isBlank(publisher) ||
               XStringUtil.isBlank(model))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "MANDATORY PARAMETER(S) CANNOT BE BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            PostApplicationResult result = null;
            try
            {
                if(null == DeviceModelManager.getInstance().getDeviceModel(model))
                {
                    status = Response.Status.NOT_FOUND;
                    errorMessage = "UNKNOWN DEVICE MODEL: " + model;

                    throw new UMEiException(
                        errorMessage,
                        status);
                }

                AppManager.getInstance().addApp(
                    publisher, 
                    name, 
                    "", 
                    model, 
                    "1",
                    1,
                    description,
                    null);

                App app = AppManager.getInstance().getApp(publisher, name, model);
                
                result = new PostApplicationResult();
                result.setApplicationId(app.getId());
            }
            catch(DemeterException e)
            {
                status = Response.Status.FORBIDDEN;
                errorMessage = e.getMessage();

                throw new UMEiException(
                    errorMessage,
                    status);
            }

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

        log.info("({},{},{},{},{},{}); {}",
            userId,
            sessionId,
            name,
            publisher,
            model,
            description,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    @PUT
    @Path("application/{targetId}")
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

        String description;

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

                PutApplicationRequest request = bodyPayload.getDesire(
                    PutApplicationRequest.class);

                description = request.getDescription();
            }
            catch(Throwable ignored)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID REQUEST PAYLOAD: " + requestPayload;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(XStringUtil.isBlank(description))
            {
                description = XStringUtil.BLANK;
            }

            try
            {
                App app = AppManager.getInstance().getApp(targetId);
                if(null == app)
                {
                    status = Response.Status.NOT_FOUND;
                    errorMessage = "APPLICATION CANNOT BE FOUND";

                    throw new UMEiException(
                        errorMessage,
                        status);
                }

                app.setDescription(description);
                
                AppManager.getInstance().setApp(app, null);
            }
            catch(DemeterException e)
            {
                status = Response.Status.FORBIDDEN;
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
            description,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    @DELETE
    @Path("application/{targetId}")
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
            try
            {
                AppManager.getInstance().deleteApp(targetId);
            }
            catch(DemeterException e)
            {
                status = Response.Status.FORBIDDEN;
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

    public static class PostApplicationRequest
    {
        private String name;
        private String publisher;
        private String model;
        private String description;

        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }
        public String getPublisher()
        {
            return publisher;
        }
        public void setPublisher(String publisher)
        {
            this.publisher = publisher;
        }
        public String getModel()
        {
            return model;
        }
        public void setModel(String model)
        {
            this.model = model;
        }
        public String getDescription()
        {
            return description;
        }
        public void setDescription(String description)
        {
            this.description = description;
        }
    }
    
    public static class PostApplicationResult
    {
        private String applicationId;

        public String getApplicationId()
        {
            return applicationId;
        }

        public void setApplicationId(String applicationId)
        {
            this.applicationId = applicationId;
        }
    }

    public static class PutApplicationRequest
    {
        private String description;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }
}
