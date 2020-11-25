package com.sercomm.openfire.plugin.exception;

import javax.ws.rs.core.Response.Status;

public class ServiceAPIException extends RuntimeException
{
    private static final long serialVersionUID = 3901306126916979047L;

    private String message;
    private Status errStatus;
    private Status rtnStatus = Status.OK;

    public ServiceAPIException()
    {
    }

    public ServiceAPIException(String message, Status errStatus)
    {
        super(message);
        this.message = message;
        this.errStatus = errStatus;
    }
    
    public ServiceAPIException(String message, Status errStatus, Status rtnStatus)
    {
        super(message);
        this.message = message;
        this.errStatus = errStatus;
        this.rtnStatus = rtnStatus;
    }

    public String getMessage()
    {
        return message;
    }
    public void setMessage(String message)
    {
        this.message = message;
    }
    public Status getErrStatus()
    {
        return errStatus;
    }

    public void setErrStatus(Status errStatus)
    {
        this.errStatus = errStatus;
    }

    public Status getRtnStatus()
    {
        return rtnStatus;
    }

    public void setRtnStatus(Status rtnStatus)
    {
        this.rtnStatus = rtnStatus;
    }
}
