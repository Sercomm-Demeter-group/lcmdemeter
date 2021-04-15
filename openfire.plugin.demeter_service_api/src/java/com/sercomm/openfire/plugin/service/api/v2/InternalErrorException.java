package com.sercomm.openfire.plugin.service.api.v2;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import com.sercomm.commons.umei.BodyPayload;

public class InternalErrorException extends Throwable implements ExceptionMapper<Throwable>
{
    private static final long serialVersionUID = 1L;
    
    public InternalErrorException()
    {
        super();
    }
    
    public InternalErrorException(String message)
    {
        super(message);
    }
    
    public InternalErrorException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InternalErrorException(Throwable cause)
    {
        super(cause);
    }

    @Override
    public Response toResponse(Throwable exception)
    {
        Response response = null;

        if(exception instanceof InternalErrorException)
        {
            InternalErrorException e = (InternalErrorException) exception;

            BodyPayload resultPayload = new BodyPayload()
                    .withError(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "OH OH...SOMETHING GOES WRONG: " + e.getMessage());

            response = Response
                    .status(Status.OK)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(resultPayload.toString())
                    .build();
        }

        return response;
    }

}
