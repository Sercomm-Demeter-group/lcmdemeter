package com.sercomm.openfire.plugin.data.frontend;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AppIcon
{
    private String appId;
    private String iconId;
    private Long size;
    private Long updatedTime;
    private byte[] data;
    
    public static AppIcon from(ResultSet rs)
    throws SQLException
    {
        AppIcon object = new AppIcon();
        
        object.appId = rs.getString("appId");
        object.iconId = rs.getString("iconId");
        object.size = rs.getLong("size");
        object.updatedTime = rs.getLong("updatedTime");
        object.data = rs.getBytes("data");
        
        return object;
    }
    
    public String getAppId()
    {
        return this.appId;
    }
    
    public void setAppId(String appId)
    {
        this.appId = appId;
    }
    
    public String getIconId()
    {
        return this.iconId;
    }
    
    public void setIconId(String iconId)
    {
        this.iconId = iconId;
    }
    
    public Long getSize()
    {
        return this.size;
    }
    
    public void setSize(Long size)
    {
        this.size = size;
    }
    
    public Long getUpdatedTime()
    {
        return this.updatedTime;
    }
    
    public void setUpdatedTime(Long updatedTime)
    {
        this.updatedTime = updatedTime;
    }
    
    public byte[] getData()
    {
        return this.data;
    }
    
    public void setData(byte[] data)
    {
        this.data = data;
    }
}
