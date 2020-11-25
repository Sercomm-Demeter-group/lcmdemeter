package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractResult;

public class UninstallAppResult extends AbstractResult
{
    public UninstallAppResult()
    {        
    }

    protected UninstallAppResult withRequestId(String requestId)
    {
        super.requestId = requestId;
        return this;
    }
    
    protected UninstallAppResult withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    protected UninstallAppResult withReceiverId(String receiverId)
    {
        super.receiverId = receiverId;
        return this;
    }
    
    protected UninstallAppResult withStatusCode(int statusCode)
    {
        super.statusCode = statusCode;
        return this;
    }
    
    protected UninstallAppResult withBodyPayload(BodyPayload bodyPayload)
    {
        super.bodyPayload = bodyPayload;
        return this;
    }
}
