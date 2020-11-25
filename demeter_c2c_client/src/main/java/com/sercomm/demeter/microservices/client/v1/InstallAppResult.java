package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractResult;

public class InstallAppResult extends AbstractResult
{
    public InstallAppResult()
    {        
    }

    protected InstallAppResult withRequestId(String requestId)
    {
        super.requestId = requestId;
        return this;
    }
    
    protected InstallAppResult withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    protected InstallAppResult withReceiverId(String receiverId)
    {
        super.receiverId = receiverId;
        return this;
    }
    
    protected InstallAppResult withStatusCode(int statusCode)
    {
        super.statusCode = statusCode;
        return this;
    }
    
    protected InstallAppResult withBodyPayload(BodyPayload bodyPayload)
    {
        super.bodyPayload = bodyPayload;
        return this;
    }
}
