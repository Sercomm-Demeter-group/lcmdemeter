package com.sercomm.openfire.plugin.service.filter;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.EndUserManager;
import com.sercomm.openfire.plugin.ServiceSessionManager;
import com.sercomm.openfire.plugin.cache.EndUserCache;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;
import com.sercomm.openfire.plugin.define.HttpHeader;

@Provider
@PreMatching
public class ServiceAuthFilter implements ContainerRequestFilter
{
    @Override
    public void filter(
            ContainerRequestContext requestContext)
    throws IOException
    {
        do
        {
            String uriPath = requestContext.getUriInfo().getPath();
            if(XStringUtil.isBlank(uriPath))
            {
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                    .entity("AUTHENTICATION REQUIRED")
                    .build());
                break;
            }

            if(uriPath.endsWith("auth") || 
               uriPath.startsWith("api/v1/files"))
            {
                break;
            }
            
            final String sessionId = requestContext.getHeaderString(HttpHeader.X_AUTH_TOKEN);        
            if(XStringUtil.isBlank(sessionId))
            {
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                    .entity("AUTHENTICATION REQUIRED")
                    .build());
                break;
            }
            
            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            if(null == session)
            {
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                    .entity("AUTHENTICATION REQUIRED")
                    .build());
                break;
            }
            
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
                    Response.status(Response.Status.FORBIDDEN)
                    .entity("ACCOUNT WAS FREEZED")
                    .build());
                break;
            }
        }
        while(false);
    }
}
