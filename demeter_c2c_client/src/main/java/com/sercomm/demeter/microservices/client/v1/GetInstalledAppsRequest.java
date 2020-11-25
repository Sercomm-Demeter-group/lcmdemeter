package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.demeter.microservices.client.AbstractRequest;

public class GetInstalledAppsRequest extends AbstractRequest
{    
    private String nodeName;

    public String getNodeName()
    {
        return this.nodeName;
    }

    public GetInstalledAppsRequest withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    public GetInstalledAppsRequest withNodeName(String nodeName)
    {
        this.nodeName = nodeName;
        return this;
    }
}
