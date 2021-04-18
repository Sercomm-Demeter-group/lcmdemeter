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
import com.sercomm.openfire.plugin.EndUserManager;
import com.sercomm.openfire.plugin.cache.EndUserCache;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(UserAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN})
public class UserAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(UserAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";
    
    @Context 
    private HttpServletRequest request;

    @POST
    @Path("user")
    @Produces({MediaType.APPLICATION_JSON})
    public Response post(String requestPayload)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String name;
        String password;
        String role;

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

                PostUserRequest request = bodyPayload.getDesire(
                    PostUserRequest.class);

                name = request.getName();
                password = request.getPassword();
                role = request.getRole();                
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
               XStringUtil.isBlank(password) ||
               XStringUtil.isBlank(role))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "MANDATORY PARAMETER(S) CANNOT BE BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            EndUserRole endUserRole = EndUserRole.fromString(role);
            if(null == endUserRole)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INVALID 'role' PARAMETER";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(EndUserManager.getInstance().isRegisteredUser(name))
            {
                status = Response.Status.FORBIDDEN;
                errorMessage = "NAME ALREADY EXISTED";

                throw new UMEiException(
                    errorMessage,
                    status);
            }
            
            EndUserCache endUserCache = 
                    EndUserManager.getInstance().createUser(name, password, endUserRole);
            
            PostUserResult result = new PostUserResult();
            result.setUserId(endUserCache.getUUID());

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

        log.info("({},{},{},{},{}); {}",
            userId,
            sessionId,
            name,
            password,
            role,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    @PUT
    @Path("user/{targetId}")
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

        String password;
        String role;

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

                PutUserRequest request = bodyPayload.getDesire(
                    PutUserRequest.class);
                
                password = request.getPassword();
                role = request.getRole();
            }
            catch(Throwable ignored)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID REQUEST PAYLOAD: " + requestPayload;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(XStringUtil.isBlank(password) ||
               XStringUtil.isBlank(role))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "MANDATORY PARAMETER(S) CANNOT BE BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);                
            }

            EndUserRole endUserRole = EndUserRole.fromString(role);
            if(null == endUserRole)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INVALID 'role' PARAMETER";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(!EndUserManager.getInstance().isRegisteredUserById(targetId))
            {
                status = Response.Status.NOT_FOUND;
                errorMessage = "ID CANNOT BE FOUND";

                throw new UMEiException(
                    errorMessage,
                    status);                
            }

            EndUserCache endUserCache = EndUserManager.getInstance().getUserById(targetId);
            final String name = endUserCache.getId();

            // perform update
            EndUserManager.getInstance().setUser(
                name,
                password,
                endUserRole,
                1);

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

        log.info("({},{},{},{},{}); {}",
            userId,
            sessionId,
            password,
            role,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    @DELETE
    @Path("user/{targetId}")
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
            if(!EndUserManager.getInstance().isRegisteredUserById(targetId))
            {
                status = Response.Status.NOT_FOUND;
                errorMessage = "ID CANNOT BE FOUND";

                throw new UMEiException(
                    errorMessage,
                    status);                
            }

            EndUserCache endUserCache = EndUserManager.getInstance().getUserById(targetId);
            final String name = endUserCache.getId();
            if(0 == name.compareTo("admin"))
            {
                status = Response.Status.NOT_ACCEPTABLE;
                errorMessage = "SPECIFIC ACCOUNT CANNOT BE DELETED";

                throw new UMEiException(
                    errorMessage,
                    status);                
            }
            
            // perform deletion
            EndUserManager.getInstance().deleteUser(name);
            
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

        log.info("({},{},{},{},{}); {}",
            userId,
            sessionId,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    public static class PostUserRequest
    {
        private String name;
        private String password;
        private String role;

        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }
        public String getPassword()
        {
            return password;
        }
        public void setPassword(String password)
        {
            this.password = password;
        }
        public String getRole()
        {
            return role;
        }
        public void setRole(String role)
        {
            this.role = role;
        }
    }
    
    public static class PostUserResult
    {
        private String userId;

        public String getUserId()
        {
            return userId;
        }

        public void setUserId(String userId)
        {
            this.userId = userId;
        }
    }
    
    public static class PutUserRequest
    {
        private String password;
        private String role;

        public String getPassword()
        {
            return password;
        }
        public void setPassword(String password)
        {
            this.password = password;
        }
        public String getRole()
        {
            return role;
        }
        public void setRole(String role)
        {
            this.role = role;
        }
    }
}
