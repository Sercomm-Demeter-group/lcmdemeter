package com.sercomm.openfire.plugin.service.api.v2;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
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

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.data.frontend.AppVersion;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(AppVersionAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR})
public class AppVersionAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(AppVersionAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    @Context
    private HttpServletRequest request;

    @POST
    @Path("application/{applicationId}/version")
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON})
    public Response post(
            @PathParam("applicationId") String applicationId,
            @FormDataParam("payload") String requestPayload,
            @FormDataParam("file") InputStream fileInputStream,
            @FormDataParam("file") FormDataContentDisposition fdcd)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String name;
        Integer size = 0;
        
        String errorMessage = XStringUtil.BLANK;
        try
        {
            // check if the application ID is valid
            try
            {
                AppManager.getInstance().getApp(applicationId);
            }
            catch(DemeterException e)
            {
                status = Response.Status.FORBIDDEN;
                errorMessage = e.getMessage();

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(XStringUtil.isBlank(requestPayload))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "MANDATORY FORM DATA MISSING";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            try
            {
                BodyPayload bodyPayload = Json.mapper().readValue(
                    requestPayload,
                    BodyPayload.class);

                PostAppVersionRequest request = bodyPayload.getDesire(
                    PostAppVersionRequest.class);
                
                name = request.getName();
            }
            catch(Throwable ignored)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID REQUEST PAYLOAD: " + requestPayload;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(XStringUtil.isBlank(name))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "MANDATORY PARAMETER(S) CANNOT BE BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int length = 0;
            byte[] buffer = new byte[1024];
            while ((length = fileInputStream.read(buffer, 0, buffer.length)) != -1)
            {
                baos.write(buffer, 0, length);
            }
            baos.flush();

            // obtain the file data
            byte[] bufferArray = baos.toByteArray();
            // obtain the file size
            size = bufferArray.length;

            PostAppVersionResult result = null;
            try
            {
                AppManager.getInstance().addAppVersion(
                    applicationId,
                    name,
                    1,
                    fdcd.getFileName(),
                    bufferArray);
                
                AppVersion appVersion = AppManager.getInstance().getAppVersion(
                    applicationId,
                    name);

                result = new PostAppVersionResult();
                result.setVersionId(appVersion.getId());
            }
            catch(DemeterException e)
            {
                throw new UMEiException(
                    e.getMessage(),
                    Response.Status.FORBIDDEN);
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

        log.info("({},{},{},{}); {}",
            userId,
            sessionId,
            name,
            size,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    @PUT
    @Path("application/{applicationId}/version/{versionId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response put(
            @PathParam("applicationId") String applicationId,
            @PathParam("versionId") String versionId,
            @FormDataParam("payload") String requestPayload,
            @FormDataParam("file") InputStream fileInputStream,
            @FormDataParam("file") FormDataContentDisposition fdcd)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String name = XStringUtil.BLANK;
        String statusString = XStringUtil.BLANK;
        Integer size = 0;

        String errorMessage = XStringUtil.BLANK;
        try
        {
            // check if the application ID is valid
            try
            {
                AppManager.getInstance().getApp(applicationId);
            }
            catch(DemeterException e)
            {
                status = Response.Status.FORBIDDEN;
                errorMessage = e.getMessage();

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            Integer statusValue = 0;
            if(!XStringUtil.isBlank(requestPayload))
            {
                BodyPayload bodyPayload = Json.mapper().readValue(
                    requestPayload,
                    BodyPayload.class);

                PutAppVersionRequest request = bodyPayload.getDesire(
                    PutAppVersionRequest.class);
                
                name = request.getName();
                statusString = request.getStatus();
                
                switch(statusString)
                {
                    case "enable":
                        statusValue = 1;
                        break;
                    case "disable":
                        statusValue = 0;
                        break;
                    default:
                        throw new UMEiException(
                            "INVALID 'status' value",
                            Response.Status.BAD_REQUEST);
                }
            }

            byte[] bufferArray = null;
            if(null != fileInputStream)
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int length = 0;
                byte[] buffer = new byte[1024];
                while ((length = fileInputStream.read(buffer, 0, buffer.length)) != -1)
                {
                    baos.write(buffer, 0, length);
                }
                baos.flush();

                // obtain the file data
                bufferArray = baos.toByteArray();
                // obtain the file size
                size = bufferArray.length;
            }

            try
            {
                AppManager.getInstance().updateAppVersion(
                    applicationId,
                    versionId,
                    name,
                    statusValue,
                    null != fdcd ? fdcd.getFileName() : XStringUtil.BLANK,
                    0 != size ? bufferArray : null);
            }
            catch(DemeterException e)
            {
                throw new UMEiException(
                    e.getMessage(),
                    Response.Status.FORBIDDEN);
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

        log.info("({},{},{},{},{},{}); {}",
            userId,
            sessionId,
            applicationId,
            versionId,
            name,
            size,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    @DELETE
    @Path("application/{applicationId}/version/{versionId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response delete(
            @PathParam("applicationId") String applicationId,
            @PathParam("versionId") String versionId)
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
                // check if the application ID is valid
                AppManager.getInstance().getApp(applicationId);
                
                // delete specific version of the application
                AppManager.getInstance().deleteAppVersion(applicationId, versionId);
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

        log.info("({},{}); {}",
            userId,
            sessionId,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    public static class PostAppVersionRequest
    {
        private String name;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }
    
    public static class PostAppVersionResult
    {
        private String versionId;

        public String getVersionId()
        {
            return versionId;
        }

        public void setVersionId(String versionId)
        {
            this.versionId = versionId;
        }
    }
    

    public static class PutAppVersionRequest
    {
        private String name;
        private String status;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
        
        public String getStatus()
        {
            return this.status;
        }
        
        public void setStatus(String status)
        {
            this.status = status;
        }
    }
}
