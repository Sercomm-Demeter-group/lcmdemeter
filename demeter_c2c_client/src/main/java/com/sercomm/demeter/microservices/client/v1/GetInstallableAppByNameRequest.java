package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.demeter.microservices.client.AbstractRequest;

public class GetInstallableAppByNameRequest extends AbstractRequest
{
    private String model = null;
    private String appName = null;
    private String appPublisher = null;

    public String getModel()
    {
        return this.model;
    }

    public String getAppName()
    {
        return this.appName;
    }

    public String getAppPublisher()
    {
        return this.appPublisher;
    }

    public GetInstallableAppByNameRequest withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }

    public GetInstallableAppByNameRequest withModel(String appPublisher)
    {
        this.appPublisher = appPublisher;
        return this;
    }

    public GetInstallableAppByNameRequest withAppName(String appName)
    {
        this.appName = appName;
        return this;
    }

    public GetInstallableAppByNameRequest withAppPublisher(String appPublisher)
    {
        this.appPublisher = appPublisher;
        return this;
    }
}
