package com.sercomm.demeter.microservices.client.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractRequest;

public class PostUbusCommandRequest extends AbstractRequest
{    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestData
    {
        private String method;
        private String path;
        private String payloadString;

        public String getMethod()
        {
            return this.method;
        }

        public void setMethod(String method)
        {
            this.method = method;
        }
        public String getPath()
        {
            return this.path;
        }
        public void setPath(String path)
        {
            this.path = path;
        }
        public String getPayloadString()
        {
            return payloadString;
        }
        public void setPayloadString(String payloadString)
        {
            this.payloadString = payloadString;
        }
    }

    private String nodeName;

    public String getNodeName()
    {
        return this.nodeName;
    }

    public PostUbusCommandRequest withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    public PostUbusCommandRequest withNodeName(String nodeName)
    {
        this.nodeName = nodeName;
        return this;
    }
    
    public PostUbusCommandRequest withRequestContents(
            String method, 
            String path, 
            String payloadString)
    {
        RequestData requestData = new RequestData();
        requestData.method = method;
        requestData.path = path;
        requestData.payloadString = payloadString;
        
        super.bodyPayload = new BodyPayload()
                .withDesire(requestData);
        
        return this;
    }
}
