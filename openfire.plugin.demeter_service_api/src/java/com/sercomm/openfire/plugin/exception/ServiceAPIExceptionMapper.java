package com.sercomm.openfire.plugin.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class ServiceAPIExceptionMapper implements ExceptionMapper<ServiceAPIException>
{

    @Override
    public Response toResponse(
            ServiceAPIException exception)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
