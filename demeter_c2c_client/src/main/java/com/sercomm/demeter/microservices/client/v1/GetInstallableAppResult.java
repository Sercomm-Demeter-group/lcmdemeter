package com.sercomm.demeter.microservices.client.v1;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractResult;

public class GetInstallableAppResult extends AbstractResult
{
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResultData
    {
        private String appName;
        private String publisher;
        private String appId;
        private String creationTime;
        private List<Version> versions;
        
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
        public String getCreationTime()
        {
            return creationTime;
        }
        public void setCreationTime(String creationTime)
        {
            this.creationTime = creationTime;
        }
        public List<Version> getVersions()
        {
            return this.versions;
        }
        public void setVersions(List<Version> versions)
        {
            this.versions = versions;
        }

        public static class Version
        {
            private String versionName;
            private String versionId;
            private String creationTime;
            
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
            public String getCreationTime()
            {
                return creationTime;
            }
            public void setCreationTime(String creationTime)
            {
                this.creationTime = creationTime;
            }
        }
    }
    
    public ResultData getData()
    {
        return super.bodyPayload.getData(ResultData.class);
    }

    protected GetInstallableAppResult withRequestId(String requestId)
    {
        super.requestId = requestId;
        return this;
    }
    
    protected GetInstallableAppResult withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    protected GetInstallableAppResult withReceiverId(String receiverId)
    {
        super.receiverId = receiverId;
        return this;
    }
    
    protected GetInstallableAppResult withStatusCode(int statusCode)
    {
        super.statusCode = statusCode;
        return this;
    }
    
    protected GetInstallableAppResult withBodyPayload(BodyPayload bodyPayload)
    {
        super.bodyPayload = bodyPayload;
        return this;
    }
}
