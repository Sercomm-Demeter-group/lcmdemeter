package com.sercomm.demeter.microservices.client.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractRequest;

public class PostEchoRequest extends AbstractRequest
{    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestData
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

    public PostEchoRequest withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    public PostEchoRequest withMessage(String message)
    {
        RequestData requestData = new RequestData();
        requestData.message = message;

        if(null == super.bodyPayload)
        {
            super.bodyPayload = new BodyPayload();
        }        
        super.bodyPayload.withDesire(requestData);
        
        return this;
    }
}
