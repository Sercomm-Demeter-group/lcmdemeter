package com.sercomm.openfire.plugin.service.filter;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.container.ContainerRequestFilter;

import com.sercomm.openfire.plugin.ServiceSessionManager;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.ServiceAPIException;
import com.sercomm.openfire.plugin.service.annotation.RequiresRoles;

public interface PermissionFilter extends ContainerRequestFilter
{
    // Extract the roles from the annotated element
    default List<EndUserRole> extractRoles(AnnotatedElement annotatedElement)
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

    // Check if the user contains one of the allowed roles
    // Throw an ServiceAPIException if the user has not permission to execute the method
    default void checkPermissions(String sessionId, List<EndUserRole> allowedRoles)
    throws ServiceAPIException
    {
        if(!allowedRoles.isEmpty())
        {
            ServiceSessionCache cache = ServiceSessionManager.getInstance().getSession(sessionId);
            EndUserRole endUserRole = cache.getEndUserRole();
            if(false == allowedRoles.contains(endUserRole))
            {
                throw new ServiceAPIException();
            }
        }
    }

}
