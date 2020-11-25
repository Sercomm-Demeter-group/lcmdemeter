package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.commons.util.XStringUtil;
import com.sercomm.demeter.microservices.client.AbstractRequest;

public class GetInstallableAppsRequest extends AbstractRequest
{
    private String model = null;
    private Integer from = null;
    private Integer size = null;
    private String sort = null;

    public String getModel()
    {
        return this.model;
    }
    
    public Integer getFrom()
    {
        return this.from;
    }
    
    public Integer getSize()
    {
        return this.size;
    }
    
    public String getSort()
    {
        return this.sort;
    }

    public GetInstallableAppsRequest withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }

    public GetInstallableAppsRequest withModel(String model)
    {
        this.model = model;
        return this;
    }
    
    public GetInstallableAppsRequest withFrom(Integer from)
    {
        this.from = from;
        return this;
    }
    
    public GetInstallableAppsRequest withSize(Integer size)
    {
        this.size = size;
        return this;
    }
    
    public GetInstallableAppsRequest withSort(String attribute, String order)
    {
        if(XStringUtil.isBlank(order))
        {
            order = "asc";
        }
        
        this.sort = attribute + ":" + order;
        return this;
    }
}
