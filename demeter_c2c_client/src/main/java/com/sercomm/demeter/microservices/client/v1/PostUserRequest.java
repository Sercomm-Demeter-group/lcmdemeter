package com.sercomm.demeter.microservices.client.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.demeter.microservices.client.AbstractRequest;

public class PostUserRequest extends AbstractRequest
{    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestData
    {
        private String username;
        private String password;
        private String role;
        
        public String getUsername()
        {
            return this.username;
        }
        public void setUsername(String username)
        {
            this.username = username;
        }
        public String getPassword()
        {
            return password;
        }
        public void setPassword(String password)
        {
            this.password = password;
        }
        public String getRole()
        {
            return role;
        }
        public void setRole(String role)
        {
            this.role = role;
        }
    }

    public PostUserRequest withOriginatorId(String originatorId)
    {
        super.originatorId = originatorId;
        return this;
    }
    
    public PostUserRequest withRequestContents(
            String username,
            String password,
            String role)
    {
        RequestData requestData = new RequestData();
        requestData.username = username;
        requestData.password = password;
        requestData.role = role;
        
        if(null == super.bodyPayload)
        {
            super.bodyPayload = new BodyPayload();
        }        
        super.bodyPayload.withDesire(requestData);
        
        return this;
    }
}
