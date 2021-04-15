package com.sercomm.openfire.plugin.service.api.v2;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import com.sercomm.commons.util.XStringUtil;
import com.sercomm.commons.umei.BodyPayload;

public class UMEiException extends RuntimeException implements ExceptionMapper<RuntimeException>
{
    private static final long serialVersionUID = 1L;

    private String message = XStringUtil.BLANK;
    private Status responseStatus = Status.OK;
    private Status errorStatus = Status.BAD_REQUEST;

    public UMEiException()
    {
        super();
    }
    
    public UMEiException(
            String message, 
            Status errStatus)
    {
        super(message);
        this.message = message;
        this.errorStatus = errStatus;
    }

    public UMEiException(
            String message, 
            Status errorStatus, 
            Status responseStatus)
    {
        super(message);
        this.message = message;
        this.errorStatus = errorStatus;
        this.responseStatus = responseStatus;
    }
    
    public String getMessage()
    {
        return message;
    }
    
    public Status getErrorStatus()
    {
        return errorStatus;
    }

    public Status getResponseStatus()
    {
        return responseStatus;
    }

    @Override
    public Response toResponse(
            RuntimeException exception)
    {
        Response response = null;
        
        if(exception instanceof UMEiException)
        {
            UMEiException e = (UMEiException) exception;

            BodyPayload resultPayload = new BodyPayload()
                    .withError(e.getErrorStatus().getStatusCode(), e.getMessage());
                
            response = Response
                    .status(e.getResponseStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(resultPayload.toString())
                    .build();
        }
        
        return response;
    }
}
