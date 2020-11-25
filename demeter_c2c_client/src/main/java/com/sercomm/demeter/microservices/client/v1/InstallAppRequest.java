package com.sercomm.demeter.microservices.client.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractRequest;

public class InstallAppRequest extends AbstractRequest
{    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestData
    {
        private String appId;
        private String versionId;
        private String taskId;

        public String getAppId()
        {
            return this.appId;
        }
        public void setAppId(String appId)
        {
            this.appId = appId;
        }
        public String getVersionId()
        {
            return this.versionId;
        }
        public void setVersionId(String versionId)
        {
            this.versionId = versionId;
        }
        public String getTaskId()
        {
            return taskId;
        }
        public void setTaskId(String taskId)
        {
            this.taskId = taskId;
        }
    }

    private String nodeName;

    public String getNodeName()
    {
        return this.nodeName;
    }

    public InstallAppRequest withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    public InstallAppRequest withNodeName(String nodeName)
    {
        this.nodeName = nodeName;
        return this;
    }
    
    public InstallAppRequest withRequestContents(
            String appId, 
            String versionId, 
            String taskId)
    {
        RequestData requestData = new RequestData();
        requestData.appId = appId;
        requestData.versionId = versionId;
        requestData.taskId = taskId;
        
        super.bodyPayload = new BodyPayload()
                .withDesire(requestData);
        
        return this;
    }
}
