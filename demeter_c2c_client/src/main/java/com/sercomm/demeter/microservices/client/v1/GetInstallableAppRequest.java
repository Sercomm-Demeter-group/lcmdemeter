package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.demeter.microservices.client.AbstractRequest;

public class GetInstallableAppRequest extends AbstractRequest
{
    private String appId = null;

    public String getAppId()
    {
        return this.appId;
    }

    public GetInstallableAppRequest withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }

    public GetInstallableAppRequest withAppId(String appId)
    {
        this.appId = appId;
        return this;
    }
}
