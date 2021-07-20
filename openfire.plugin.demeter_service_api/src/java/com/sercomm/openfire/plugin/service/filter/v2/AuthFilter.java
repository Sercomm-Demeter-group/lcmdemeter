package com.sercomm.openfire.plugin.service.filter.v2;

import java.io.IOException;
import java.util.Date;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sercomm.commons.util.HttpUtil;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.EndUserManager;
import com.sercomm.openfire.plugin.ServiceSessionManager;
import com.sercomm.openfire.plugin.cache.EndUserCache;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter
{
    @Override
    public void filter(
            ContainerRequestContext requestContext)
    throws IOException
    {
        do
        {
            final String method = requestContext.getMethod();
            final String uriPath = requestContext.getUriInfo().getPath();
            if(XStringUtil.isBlank(uriPath))
            {
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED).build());
                break;
            }

            // API v2 authorization
            if(!uriPath.startsWith("api/v2/"))
            {
                break;
            }

            // skip the following URIs
            if(uriPath.startsWith("api/v2/session") &&
               0 == method.compareToIgnoreCase(HttpUtil.METHOD_POST))
            {
                break;
            }

            String authField = requestContext.getHeaderString(HttpUtil.HEADER_AUTHORIZATION);        
            if(XStringUtil.isBlank(authField))
            {
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED).build());
                break;
            }                
            
            // obtain JWT token
            String[] tokens = authField.split(" ", 2);
            if(2 != tokens.length || 0 != "Bearer".compareTo(tokens[0]))
            {
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED).build());
                break;
            }

            // verify JWT
            /*
             {
                 "sub": "1234567890",
                 "name": "admin",
                 "role": "admin",
                 "iat": 1516239022,
                 "exp": 1516325422
             }
             */
            // decode the JWT at first for obtaining session ID
            DecodedJWT jwt;
            String sessionId;
            try
            {
                jwt = JWT.decode(tokens[1]);

                sessionId = jwt.getSubject().toString();
                if(1 <= new Date().compareTo(jwt.getExpiresAt()))
                {
                    requestContext.abortWith(
                        Response.status(Response.Status.UNAUTHORIZED).build());
                    break;
                }
            }
            catch(Throwable t)
            {
                // invalid JWT format
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED).build());
                break;
            }

            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            if(null == session)
            {
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED).build());
                break;
            }

            try
            {
                Algorithm algorithm = Algorithm.HMAC256(session.getStoredKey());
                JWTVerifier verifier = JWT.require(algorithm).build();
                verifier.verify(jwt);
            }
            catch(JWTVerificationException e)
            {
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED).build());
                break;
            }

            // authorize successfully
            // update the session data for renewing its alive time
            ServiceSessionManager.getInstance().updateSession(session);
            // attach necessary data to the request context
            requestContext.setProperty("jwt", tokens[1]);
            requestContext.setProperty("sessionId", sessionId);
            requestContext.setProperty("userId", session.getUserId());

            try
            {
                EndUserCache endUser = EndUserManager.getInstance().getUser(session.getUserId());
                if(0 == endUser.getValid())
                {
                    throw new Throwable();
                }
            }
            catch(Throwable ignored)
            {
                requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN).build());
                break;
            }
        }
        while(false);
    }
}
