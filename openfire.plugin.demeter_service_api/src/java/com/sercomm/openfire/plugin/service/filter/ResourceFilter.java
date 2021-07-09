package com.sercomm.openfire.plugin.service.filter;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

@Provider
public class ResourceFilter implements ContainerResponseFilter
{
    @Context 
    private UriInfo uriInfo;

    @Override
    public void filter(
        ContainerRequestContext requestContext, 
        ContainerResponseContext responseContext)
    throws IOException 
    {
        List<Object> matchedClasses = uriInfo.getMatchedResources();

        // since Jersey responds HTTP 204 by default if no request handler is available,
        // instead of HTTP 204, response code is desired to be HTTP 404
        if(matchedClasses.size() == 0)
        {
            // in this case, HTTP 401 and HTTP 405 should not be changed
            if(responseContext.getStatus() != Response.Status.UNAUTHORIZED.getStatusCode() &&
               responseContext.getStatus() != Response.Status.METHOD_NOT_ALLOWED.getStatusCode())
            {
                // respond HTTP 404
                responseContext.setStatus(Response.Status.NOT_FOUND.getStatusCode());
            }
        }
    }
}
