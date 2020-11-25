package com.sercomm.openfire.plugin.data.frontend;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Ownership
{
    private String serial;
    private String mac;
    private String userId;
    private String type;  // owned, shared
    private Long creationTime;
    
    public static Ownership from(ResultSet rs)
    throws SQLException
    {
        Ownership object = new Ownership();
        
        object.setSerial(
            rs.getString("serial"));
        object.setMac(
            rs.getString("mac"));
        object.setUserId(
            rs.getString("userId"));
        object.setType(
            rs.getString("type"));
        object.setCreationTime(
            rs.getLong("creationTime"));
        
        return object;
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

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
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
