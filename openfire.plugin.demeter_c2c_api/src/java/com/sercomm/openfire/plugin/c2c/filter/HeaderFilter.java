package com.sercomm.openfire.plugin.c2c.filter;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sercomm.commons.umei.HeaderField;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.c2c.exception.UMEiException;

@Provider
@PreMatching
public class HeaderFilter implements ContainerRequestFilter
{
    @Override
    public void filter(
            ContainerRequestContext requestContext)
    throws IOException
    {
        String requestId = requestContext.getHeaderString(HeaderField.HEADER_REQUEST_ID);
        String originatorId = requestContext.getHeaderString(HeaderField.HEADER_ORIGINATOR_ID);
        
        if(XStringUtil.isBlank(requestId) || XStringUtil.isBlank(originatorId))
        {
            throw new UMEiException(
                    "HTTP HEADER 'X-Request-ID' OR 'X-Originator-ID' CANNOT BE BLANK",
                    Status.BAD_REQUEST);
        }
    }
}
