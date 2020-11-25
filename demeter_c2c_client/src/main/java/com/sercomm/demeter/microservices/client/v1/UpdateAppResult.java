package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractResult;

public class UpdateAppResult extends AbstractResult
{
    public UpdateAppResult()
    {        
    }

    protected UpdateAppResult withRequestId(String requestId)
    {
        super.requestId = requestId;
        return this;
    }
    
    protected UpdateAppResult withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    protected UpdateAppResult withReceiverId(String receiverId)
    {
        super.receiverId = receiverId;
        return this;
    }
    
    protected UpdateAppResult withStatusCode(int statusCode)
    {
        super.statusCode = statusCode;
        return this;
    }
    
    protected UpdateAppResult withBodyPayload(BodyPayload bodyPayload)
    {
        super.bodyPayload = bodyPayload;
        return this;
    }
}
