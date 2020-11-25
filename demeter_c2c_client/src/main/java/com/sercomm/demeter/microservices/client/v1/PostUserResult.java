package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractResult;

public class PostUserResult extends AbstractResult
{
    public PostUserResult()
    {        
    }

    protected PostUserResult withRequestId(String requestId)
    {
        super.requestId = requestId;
        return this;
    }
    
    protected PostUserResult withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    protected PostUserResult withReceiverId(String receiverId)
    {
        super.receiverId = receiverId;
        return this;
    }
    
    protected PostUserResult withStatusCode(int statusCode)
    {
        super.statusCode = statusCode;
        return this;
    }
    
    protected PostUserResult withBodyPayload(BodyPayload bodyPayload)
    {
        super.bodyPayload = bodyPayload;
        return this;
    }
}
