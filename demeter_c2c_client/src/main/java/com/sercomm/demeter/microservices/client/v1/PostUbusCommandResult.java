package com.sercomm.demeter.microservices.client.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractResult;

public class PostUbusCommandResult extends AbstractResult
{
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResultData
    {
        private String result;
        
        public String getResult()
        {
            return result;
        }

        public void setResult(String result)
        {
            this.result = result;
        }
    }

    public PostUbusCommandResult()
    {        
    }

    public ResultData getData()
    {
        return super.bodyPayload.getData(ResultData.class);
    }

    protected PostUbusCommandResult withRequestId(String requestId)
    {
        super.requestId = requestId;
        return this;
    }
    
    protected PostUbusCommandResult withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    protected PostUbusCommandResult withReceiverId(String receiverId)
    {
        super.receiverId = receiverId;
        return this;
    }
    
    protected PostUbusCommandResult withStatusCode(int statusCode)
    {
        super.statusCode = statusCode;
        return this;
    }
    
    protected PostUbusCommandResult withBodyPayload(BodyPayload bodyPayload)
    {
        super.bodyPayload = bodyPayload;
        return this;
    }
}
