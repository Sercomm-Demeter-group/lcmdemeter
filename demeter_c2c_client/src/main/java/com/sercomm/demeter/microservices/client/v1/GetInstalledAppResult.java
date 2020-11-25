package com.sercomm.demeter.microservices.client.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractResult;

public class GetInstalledAppResult extends AbstractResult
{
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResultData
    {
        private String appName;
        private String publisher;
        private String appId;
        private String status;
        private Version version;

        public String getAppName()
        {
            return this.appName;
        }
        public void setAppName(String appName)
        {
            this.appName = appName;
        }
        public String getPublisher()
        {
            return this.publisher;
        }
        public void setPublisher(String publisher)
        {
            this.publisher = publisher;
        }
        public String getAppId()
        {
            return this.appId;
        }
        public void setAppId(String appId)
        {
            this.appId = appId;
        }
        public String getStatus()
        {
            return this.status;
        }
        public void setStatus(String status)
        {
            this.status = status;
        }
        public Version getVersion()
        {
            return version;
        }
        public void setVersion(Version version)
        {
            this.version = version;
        }
        
        public static class Version
        {
            private String versionName;
            private String versionId;
            
            public String getVersionName()
            {
                return this.versionName;
            }
            public void setVersionName(String versionName)
            {
                this.versionName = versionName;
            }
            public String getVersionId()
            {
                return this.versionId;
            }
            public void setVersionId(String versionId)
            {
                this.versionId = versionId;
            }
        }
    }

    public GetInstalledAppResult()
    {        
    }

    public ResultData getData()
    {
        return super.bodyPayload.getData(ResultData.class);
    }

    protected GetInstalledAppResult withRequestId(String requestId)
    {
        super.requestId = requestId;
        return this;
    }
    
    protected GetInstalledAppResult withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    protected GetInstalledAppResult withReceiverId(String receiverId)
    {
        super.receiverId = receiverId;
        return this;
    }
    
    protected GetInstalledAppResult withStatusCode(int statusCode)
    {
        super.statusCode = statusCode;
        return this;
    }
    
    protected GetInstalledAppResult withBodyPayload(BodyPayload bodyPayload)
    {
        super.bodyPayload = bodyPayload;
        return this;
    }
}
