package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.demeter.microservices.client.AbstractRequest;

public class UninstallAppRequest extends AbstractRequest
{    
    private String nodeName;
    private String appId;
    
    public String getNodeName()
    {
        return this.nodeName;
    }

    public String getAppId()
    {
        return this.appId;
    }
    
    public UninstallAppRequest withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    public UninstallAppRequest withNodeName(String nodeName)
    {
        this.nodeName = nodeName;
        return this;
    }
    
    public UninstallAppRequest withAppId(String appId)
    {
        this.appId = appId;

        return this;
    }
}
