package com.sercomm.openfire.plugin.service.filter;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.sercomm.openfire.plugin.ServiceSessionManager;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.define.HttpHeader;
import com.sercomm.openfire.plugin.exception.ServiceAPIException;
import com.sercomm.openfire.plugin.service.annotation.RequiresRoles;

@RequiresRoles
@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter implements ContainerRequestFilter
{
    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(
            ContainerRequestContext requestContext)
    throws IOException
    {
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
            if(methodRoles.isEmpty()) 
            {
                checkPermissions(sessionId, classRoles);
            } 
            else
            {
                checkPermissions(sessionId, methodRoles);
            }
        } 
        catch(ServiceAPIException e) 
        {
            requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN).build());
        }
    }

    // Extract the roles from the annotated element
    private List<EndUserRole> extractRoles(AnnotatedElement annotatedElement)
    {
        if(annotatedElement == null) 
        {
            return new ArrayList<EndUserRole>();
        } 
        else 
        {
            RequiresRoles secured = annotatedElement.getAnnotation(RequiresRoles.class);
            if(secured == null) 
            {
                return new ArrayList<EndUserRole>();
            } 
            else 
            {
                EndUserRole[] allowedRoles = secured.value();
                return Arrays.asList(allowedRoles);
            }
        }
    }

    private void checkPermissions(String sessionId, List<EndUserRole> allowedRoles) 
    throws ServiceAPIException 
    {
        // Check if the user contains one of the allowed roles
        // Throw an ServiceAPIException if the user has not permission to execute the method
        ServiceSessionCache cache = ServiceSessionManager.getInstance().getSession(sessionId);
        EndUserRole endUserRole = cache.getEndUserRole();
        if(false == allowedRoles.contains(endUserRole))
        {
            throw new ServiceAPIException();
        }
    }
}
