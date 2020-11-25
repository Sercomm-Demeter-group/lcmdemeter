package com.sercomm.openfire.plugin.data.frontend;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AppEventRecord
{
    private String id;
    private String appId;
    private String appName;
    private String serial;
    private String mac;
    private String userId;
    private String eventType;
    private Long unixTime;
    private String partitionIdx;

    private Integer retryCount = 0;

    public static AppEventRecord from(ResultSet rs)
    throws SQLException
    {
        AppEventRecord object = new AppEventRecord();
        
        object.id = rs.getString("id");
        object.appId = rs.getString("appId");
        object.appName = rs.getString("appName");
        object.serial = rs.getString("serial");
        object.mac = rs.getString("mac");
        object.userId = rs.getString("userId");
        object.eventType = rs.getString("eventType");
        object.unixTime = rs.getLong("unixTime");
        object.partitionIdx = rs.getString("partitionIdx");
        
        return object;
    }

    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }
    public String getAppId()
    {
        return appId;
    }
    public void setAppId(String appId)
    {
        this.appId = appId;
    }
    public String getAppName()
    {
        return appName;
    }
    public void setAppName(String appName)
    {
        this.appName = appName;
    }
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
    public String getUserId()
    {
        return userId;
    }
    public void setUserId(String userId)
    {
        this.userId = userId;
    }
    public String getEventType()
    {
        return eventType;
    }
    public void setEventType(String eventType)
    {
        this.eventType = eventType;
    }
    public Long getUnixTime()
    {
        return unixTime;
    }
    public void setUnixTime(Long unixTime)
    {
        this.unixTime = unixTime;
    }
    public String getPartitionIdx()
    {
        return partitionIdx;
    }
    public void setPartitionIdx(String partitionIdx)
    {
        this.partitionIdx = partitionIdx;
    }
    public Integer getRetryCount()
    {
        return this.retryCount;
    }    
    public void setRetryCount(Integer retryCount)
    {
        this.retryCount = retryCount;
    }
}
