package com.sercomm.demeter.microservices.client.v1;

import com.sercomm.commons.util.XStringUtil;
import com.sercomm.demeter.microservices.client.AbstractRequest;

public class GetDevicesRequest extends AbstractRequest
{
    private String models = null;
    private String states = null;
    private Integer from = null;
    private Integer size = null;
    private String sort = null;

    public String getModels()
    {
        return this.models;
    }
    
    public String getStates()
    {
        return this.states;
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

    public GetDevicesRequest withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }

    public GetDevicesRequest withModel(String model)
    {
        if(XStringUtil.isBlank(this.models))
        {
            this.models = model;
        }
        else
        {
            this.models += ("," + model);
        }
        
        return this;
    }
    
    public GetDevicesRequest withState(String state)
    {
        if(XStringUtil.isBlank(this.states))
        {
            this.states = state;
        }
        else
        {
            this.states += ("," + state);
        }
        
        return this;
    }
    
    public GetDevicesRequest withFrom(Integer from)
    {
        this.from = from;
        return this;
    }
    
    public GetDevicesRequest withSize(Integer size)
    {
        this.size = size;
        return this;
    }
    
    public GetDevicesRequest withSort(String attribute, String order)
    {
        if(XStringUtil.isBlank(order))
        {
            order = "asc";
        }
        
        this.sort = attribute + ":" + order;
        return this;
    }
}
