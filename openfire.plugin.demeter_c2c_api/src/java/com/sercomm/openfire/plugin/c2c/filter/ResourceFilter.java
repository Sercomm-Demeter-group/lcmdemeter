package com.sercomm.openfire.plugin.c2c.filter;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class ResourceFilter implements ContainerResponseFilter
{
    @Override
    public void filter(
        ContainerRequestContext requestContext, 
        ContainerResponseContext responseContext)
    throws IOException 
    {
        Object object = responseContext.getEntity();
        // since Jersey responds HTTP 204 by default if no response entity is available,
        // instead of HTTP 204, response code is desired to be HTTP 404
        if(object == null)
        {
            responseContext.setStatus(Response.Status.NOT_FOUND.getStatusCode());
        }
    }
    
}
