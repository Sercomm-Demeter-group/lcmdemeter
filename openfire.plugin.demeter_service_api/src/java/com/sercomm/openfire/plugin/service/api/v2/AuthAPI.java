package com.sercomm.openfire.plugin.service.api.v2;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.util.DateTime;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.EndUserManager;
import com.sercomm.openfire.plugin.ServiceSessionManager;
import com.sercomm.openfire.plugin.cache.EndUserCache;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(AuthAPI.URI_PATH)
public class AuthAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(AuthAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";    

    @Context 
    private HttpServletRequest request;

    @POST
    @Path("session")
    @Produces({MediaType.APPLICATION_JSON})
    public Response post(String requestPayload)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        String name;
        String password;

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

                PostSessionRequest request = bodyPayload.getDesire(
                    PostSessionRequest.class);
                
                name = request.getName();
                password = request.getPassword();
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
               XStringUtil.isBlank(password))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "MANDATORY ARGUMENT(S) CANNOT BE BLANK";

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            if(!EndUserManager.getInstance().isRegisteredUser(name))
            {
                status = Response.Status.UNAUTHORIZED;
                errorMessage = "INVALID NAME OR PASSWORD";

                throw new UMEiException(
                    errorMessage,
                    status);                
            }

            final String plainPassword = EndUserManager.getInstance().getPassword(name);
            if(0 != plainPassword.compareTo(password))
            {
                status = Response.Status.UNAUTHORIZED;
                errorMessage = "INVALID NAME OR PASSWORD";

                throw new UMEiException(
                    errorMessage,
                    status);                
            }

            EndUserCache endUserCache = EndUserManager.getInstance().getUser(name);
            if(1 != endUserCache.getValid())
            {
                status = Response.Status.FORBIDDEN;
                errorMessage = "BLOCKED, PLEASE CONTACT ADMINISTRATOR";

                throw new UMEiException(
                    errorMessage,
                    status);                
            }

            // session ID
            final String sessionId = UUID.randomUUID().toString();

            // stored key
            final String storedKey = endUserCache.getStoredKey();

            // role
            final EndUserRole endUserRole = endUserCache.getEndUserRole();

            ServiceSessionCache cache = new ServiceSessionCache();
            cache.setUserId(name);
            cache.setSessionId(sessionId);
            cache.setStoredKey(storedKey);
            cache.setEndUserRole(endUserRole);
            ServiceSessionManager.getInstance().updateSession(cache);

            // generate JWT
            DateTime iat = DateTime.now();
            DateTime exp = DateTime.now();
            exp.addDay(1);

            Algorithm algorithm = Algorithm.HMAC256(storedKey);
            String token = JWT.create()
                    .withSubject(sessionId)
                    .withClaim("iat", iat.getTimeInSeconds())
                    .withClaim("exp", exp.getTimeInSeconds())
                    .withClaim("name", name)
                    .withClaim("role", endUserRole.toString())
                    .sign(algorithm);
            
            PostSessionResult result = new PostSessionResult();
            result.setSessionId(sessionId);
            result.setRole(endUserRole.toString());
            result.setToken(token);

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

        log.info("({},{}); {}",
            name,
            password,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }
    
    @PUT
    @Path("session")
    @Produces({MediaType.APPLICATION_JSON})
    public Response put()
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String errorMessage = XStringUtil.BLANK;
        try
        {
            EndUserCache endUserCache = EndUserManager.getInstance().getUser(userId);
            if(1 != endUserCache.getValid())
            {
                status = Response.Status.FORBIDDEN;
                errorMessage = "BLOCKED, PLEASE CONTACT ADMINISTRATOR";

                throw new UMEiException(
                    errorMessage,
                    status);                
            }

            // stored key
            final String storedKey = endUserCache.getStoredKey();

            // role
            EndUserRole endUserRole = endUserCache.getEndUserRole();

            ServiceSessionCache cache = ServiceSessionManager.getInstance().getSession(sessionId);
            // prevent from role has already been updated
            cache.setEndUserRole(endUserRole);
            ServiceSessionManager.getInstance().updateSession(cache);

            // generate JWT
            DateTime iat = DateTime.now();
            DateTime exp = DateTime.now();
            exp.addDay(1);

            Algorithm algorithm = Algorithm.HMAC256(storedKey);
            String token = JWT.create()
                    .withSubject(sessionId)
                    .withClaim("iat", iat.getTimeInSeconds())
                    .withClaim("exp", exp.getTimeInSeconds())
                    .withClaim("name", userId)
                    .withClaim("role", endUserRole.toString())
                    .sign(algorithm);
            
            PutSessionResult result = new PutSessionResult();
            result.setToken(token);

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

        log.info("({},{}); {}",
            userId,
            sessionId,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    @DELETE
    @Path("session")
    @Produces({MediaType.APPLICATION_JSON})
    public Response delete()
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String errorMessage = XStringUtil.BLANK;
        try
        {
            ServiceSessionManager.getInstance().removeSession(sessionId);
            
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
    
    public static class PostSessionRequest
    {
        private String name;
        private String password;
        
        public String getName()
        {
            return this.name;
        }
        
        public void setName(String name)
        {
            this.name = name;
        }

        public String getPassword()
        {
            return this.password;
        }
        
        public void setPassword(String password)
        {
            this.password = password;
        }
    }
    
    public static class PostSessionResult
    {
        private String role;
        private String sessionId;
        private String token;
        
        public String getRole()
        {
            return this.role;
        }
        
        public void setRole(String role)
        {
            this.role = role;
        }
        
        public String getSessionId()
        {
            return this.sessionId;
        }
        
        public void setSessionId(String sessionId)
        {
            this.sessionId = sessionId;
        }
        
        public String getToken()
        {
            return this.token;
        }
        
        public void setToken(String token)
        {
            this.token = token;
        }
    }
    
    public static class PutSessionResult
    {
        private String token;
                
        public String getToken()
        {
            return this.token;
        }
        
        public void setToken(String token)
        {
            this.token = token;
        }
    }
}
