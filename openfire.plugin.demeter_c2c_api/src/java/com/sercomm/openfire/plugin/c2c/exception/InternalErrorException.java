package com.sercomm.openfire.plugin.c2c.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import com.sercomm.commons.util.XStringUtil;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.HeaderField;

public class InternalErrorException extends Throwable implements ExceptionMapper<Throwable>
{
    private static final long serialVersionUID = 1L;

    private String requestId = XStringUtil.BLANK;
    private String originatorId = XStringUtil.BLANK;
    private String receiverId = XStringUtil.BLANK;
    
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

    public InternalErrorException withIdentifyValues(
            String requestId, 
            String originatorId, 
            String receiverId)
    {
        this.requestId = requestId;
        this.originatorId = originatorId;
        this.receiverId = receiverId;
        
        return this;
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
                    .header(HeaderField.HEADER_REQUEST_ID, e.requestId)
                    .header(HeaderField.HEADER_ORIGINATOR_ID, e.originatorId)
                    .header(HeaderField.HEADER_RECEIVER_ID, e.receiverId)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(resultPayload.toString())
                    .build();
        }

        return response;
    }

}
