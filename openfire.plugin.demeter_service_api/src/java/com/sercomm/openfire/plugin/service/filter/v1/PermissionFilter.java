package com.sercomm.openfire.plugin.service.filter.v1;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.sercomm.commons.util.HttpUtil;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.define.HttpHeader;
import com.sercomm.openfire.plugin.exception.ServiceAPIException;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class PermissionFilter implements com.sercomm.openfire.plugin.service.filter.PermissionFilter
{
    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(
            ContainerRequestContext requestContext)
    throws IOException
    {
        do
        {
            final String method = requestContext.getMethod();
            final String uriPath = requestContext.getUriInfo().getPath();
            // for 'api/v1/' only
            if(!uriPath.startsWith("api/v1/"))
            {
                break;
            }

            // skip the following URIs
            if(uriPath.startsWith("api/v1/session") &&
               0 == method.compareToIgnoreCase(HttpUtil.METHOD_POST))
            {
                break;
            }

            // Get the resource class which matches with the requested URL
            // Extract the roles declared by it
            Class<?> resourceClass = resourceInfo.getResourceClass();
            List<EndUserRole> classRoles = extractRoles(resourceClass);

            // Get the resource method which matches with the requested URL
            // Extract the roles declared by it
            Method resourceMethod = resourceInfo.getResourceMethod();
            List<EndUserRole> methodRoles = extractRoles(resourceMethod);

            String sessionId = requestContext.getHeaderString(HttpHeader.X_AUTH_TOKEN);
            try
            {
                // Check if the user is allowed to execute the method
                // The method annotations override the class annotations
                if(!methodRoles.isEmpty()) 
                {
                    checkPermissions(sessionId, methodRoles);
                } 
                else
                {
                    checkPermissions(sessionId, classRoles);
                }
            } 
            catch(ServiceAPIException e) 
            {
                requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN).build());
            }
            catch(Throwable t)
            {
                requestContext.abortWith(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
            }
        }
        while(false);
    }
}
