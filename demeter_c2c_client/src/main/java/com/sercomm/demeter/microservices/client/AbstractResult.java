package com.sercomm.demeter.microservices.client;

import java.util.List;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.Meta;
import com.sercomm.commons.umei.UMEiError;

public abstract class AbstractResult
{
    protected String requestId;
    protected String originatorId;
    protected String receiverId;
    protected int statusCode;
    protected BodyPayload bodyPayload;

    public String getRequestId()
    {
        return this.requestId;
    }

    public String getOriginatorId()
    {
        return this.originatorId;
    }

    public String getReceiverId()
    {
        return this.receiverId;
    }

    public int getStatusCode()
    {
        return this.statusCode;
    }

    public String getRawText()
    {
        return this.bodyPayload.toString();
    }

    public Meta getMeta()
    {
        return this.bodyPayload.getMeta();
    }

    public List<UMEiError> getErrors()
    {
        return this.bodyPayload.getErrors();
    }
    
    public boolean hasError()
    {
        return (null != this.getErrors() && false == this.getErrors().isEmpty());
    }
}
