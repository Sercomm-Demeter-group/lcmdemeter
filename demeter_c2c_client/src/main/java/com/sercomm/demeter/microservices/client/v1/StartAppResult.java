package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractResult;

public class StartAppResult extends AbstractResult
{
    public StartAppResult()
    {        
    }

    protected StartAppResult withRequestId(String requestId)
    {
        super.requestId = requestId;
        return this;
    }
    
    protected StartAppResult withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    protected StartAppResult withReceiverId(String receiverId)
    {
        super.receiverId = receiverId;
        return this;
    }
    
    protected StartAppResult withStatusCode(int statusCode)
    {
        super.statusCode = statusCode;
        return this;
    }
    
    protected StartAppResult withBodyPayload(BodyPayload bodyPayload)
    {
        super.bodyPayload = bodyPayload;
        return this;
    }
}
