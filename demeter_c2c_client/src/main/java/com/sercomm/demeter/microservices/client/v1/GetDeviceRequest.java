package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.demeter.microservices.client.AbstractRequest;

public class GetDeviceRequest extends AbstractRequest
{
    private String nodeName = null;

    public String getNodeName()
    {
        return this.nodeName;
    }
    
    public GetDeviceRequest withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }

    public GetDeviceRequest withNodeName(String nodeName)
    {
        this.nodeName = nodeName;
        return this;
    }
}
