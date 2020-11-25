package com.sercomm.demeter.microservices.client.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractResult;

public class PostEchoResult extends AbstractResult
{
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResultData
    {
        private String message;

        public String getMessage()
        {
            return this.message;
        }
        public void setMessage(String message)
        {
            this.message = message;
        }
    }

    public PostEchoResult()
    {        
    }

    public ResultData getData()
    {
        return super.bodyPayload.getData(ResultData.class);
    }

    protected PostEchoResult withRequestId(String requestId)
    {
        super.requestId = requestId;
        return this;
    }
    
    protected PostEchoResult withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    protected PostEchoResult withReceiverId(String receiverId)
    {
        super.receiverId = receiverId;
        return this;
    }
    
    protected PostEchoResult withStatusCode(int statusCode)
    {
        super.statusCode = statusCode;
        return this;
    }
    
    protected PostEchoResult withBodyPayload(BodyPayload bodyPayload)
    {
        super.bodyPayload = bodyPayload;
        return this;
    }
}
