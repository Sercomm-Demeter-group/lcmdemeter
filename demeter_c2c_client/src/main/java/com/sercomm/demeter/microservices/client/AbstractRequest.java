package com.sercomm.demeter.microservices.client;

import java.util.UUID;

import com.fasterxml.jackson.databind.JavaType;
import com.sercomm.commons.umei.BodyPayload;

public abstract class AbstractRequest
{
    protected String requestId;
    protected String originatorId;
    protected BodyPayload bodyPayload;
    
    public AbstractRequest()
    {
        this.requestId = UUID.randomUUID().toString();
    }

    public String getRequestId()
    {
        return requestId;
    }

    public String getOriginatorId()
    {
        return originatorId;
    }

    protected <T> T getDesire(Class<T> clazz)
    {
        return this.bodyPayload.getDesire(clazz);
    }

    protected <T> T getDesire(JavaType javaType)
    {
        return this.bodyPayload.getDesire(javaType);
    }

    protected <T> T getData(Class<T> clazz)
    {
        return this.bodyPayload.getData(clazz);
    }

    protected <T> T getData(JavaType javaType)
    {
        return this.bodyPayload.getData(javaType);
    }

    public String getRawText()
    {
        return this.bodyPayload.toString();
    }    
}
