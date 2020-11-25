package com.sercomm.demeter.microservices.client.v1;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.util.Json;
import com.sercomm.demeter.microservices.client.AbstractResult;

public class GetDevicesResult extends AbstractResult
{
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResultData
    {
        private String serial;
        private String mac;
        private String model;
        private String state;
        private String firmware;
        private String creationTime;

        public String getSerial()
        {
            return serial;
        }
        public void setSerial(String serial)
        {
            this.serial = serial;
        }
        public String getMac()
        {
            return mac;
        }
        public void setMac(String mac)
        {
            this.mac = mac;
        }
        public String getModel()
        {
            return model;
        }
        public void setModel(String model)
        {
            this.model = model;
        }
        public String getState()
        {
            return state;
        }
        public void setState(String state)
        {
            this.state = state;
        }
        public String getFirmware()
        {
            return firmware;
        }
        public void setFirmware(String firmware)
        {
            this.firmware = firmware;
        }
        public String getCreationTime()
        {
            return creationTime;
        }
        public void setCreationTime(String creationTime)
        {
            this.creationTime = creationTime;
        }
    }
    
    public ArrayList<ResultData> getData()
    {
        return super.bodyPayload.getData(
            Json.JavaTypeUtil.collectionType(ArrayList.class, ResultData.class));
    }

    protected GetDevicesResult withRequestId(String requestId)
    {
        super.requestId = requestId;
        return this;
    }
    
    protected GetDevicesResult withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    protected GetDevicesResult withReceiverId(String receiverId)
    {
        super.receiverId = receiverId;
        return this;
    }
    
    protected GetDevicesResult withStatusCode(int statusCode)
    {
        super.statusCode = statusCode;
        return this;
    }
    
    protected GetDevicesResult withBodyPayload(BodyPayload bodyPayload)
    {
        super.bodyPayload = bodyPayload;
        return this;
    }
}
