package com.sercomm.openfire.plugin.data.frontend;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AppSubscription
{
    private String appId;
    private String userId;
    private Long creationTime;
    
    public AppSubscription()
    {
    }
    
    public static AppSubscription from(ResultSet rs)
    throws SQLException
    {
        AppSubscription object = new AppSubscription();
        
        object.appId = rs.getString("appId");
        object.userId = rs.getString("userId");
        object.creationTime = rs.getLong("creationTime");
        
        return object;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public Long getCreationTime()
    {
        return creationTime;
    }

    public void setCreationTime(Long creationTime)
    {
        this.creationTime = creationTime;
    }
}
