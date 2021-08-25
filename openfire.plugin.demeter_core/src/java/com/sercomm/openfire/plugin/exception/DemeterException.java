package com.sercomm.openfire.plugin.exception;

import javax.ws.rs.core.Response.Status;

import java.io.PrintStream;
import java.io.PrintWriter;

public class DemeterException extends Exception
{
    private static final long serialVersionUID = 1L;

    private Throwable nestedThrowable = null;
    private Status http_status = null;


    public DemeterException()
    {
        super();
    }
    
    public DemeterException(String message)
    {
        super(message);
    }
    
    public DemeterException(Throwable nestedThrowable)
    {
        this.nestedThrowable = nestedThrowable;
    }
    
    public DemeterException(String message, Throwable nestedThrowable)
    {
        super(message);
        this.nestedThrowable = nestedThrowable;
    }

    public DemeterException SettHttpStatus(Status status){
        this.http_status = status;
        return this;
    }

    public Status GetHttpStatus(){
        return http_status;
    }
    
    @Override
    public void printStackTrace() 
    {
        super.printStackTrace();
        if(nestedThrowable != null) 
        {
            nestedThrowable.printStackTrace();
        }
    }
    
    @Override
    public void printStackTrace(PrintStream ps) 
    {
        super.printStackTrace(ps);
        if(nestedThrowable != null) 
        {
            nestedThrowable.printStackTrace(ps);
        }
    }

    @Override
    public void printStackTrace(PrintWriter pw) 
    {
        super.printStackTrace(pw);
        if(nestedThrowable != null) 
        {
            nestedThrowable.printStackTrace(pw);
        }
    }
}
