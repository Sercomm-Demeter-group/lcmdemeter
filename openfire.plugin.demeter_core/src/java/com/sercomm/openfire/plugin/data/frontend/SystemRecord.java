package com.sercomm.openfire.plugin.data.frontend;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SystemRecord
{
    private String id;
    private String platform;
    private String serial;
    private String mac;
    private Integer category;
    private String type;
    private Long unixTime;
    private String detail;
    
    private Integer retryCount = 0;

    public static SystemRecord from(ResultSet rs)
    throws SQLException
    {
        SystemRecord object = new SystemRecord();
        
        object.setId(rs.getString("id"));
        object.setPlatform(rs.getString("platform"));
        object.setSerial(rs.getString("serial"));
        object.setMac(rs.getString("mac"));
        object.setCategory(rs.getInt("category"));
        object.setType(rs.getString("type"));
        object.setUnixTime(rs.getLong("unixTime"));
        object.setDetail(rs.getString("detail"));
        
        return object;
    }
    
    public SystemRecord()
    {
    }

    public String getId()
    {
        return id;
    }

    public void setId(
            String id)
    {
        this.id = id;
    }

    public String getPlatform()
    {
        return platform;
    }

    public void setPlatform(String platform)
    {
        this.platform = platform;
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

    public Integer getCategory()
    {
        return category;
    }

    public void setCategory(Integer category)
    {
        this.category = category;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public Long getUnixTime()
    {
        return unixTime;
    }

    public void setUnixTime(Long unixTime)
    {
        this.unixTime = unixTime;
    }

    public String getDetail()
    {
        return detail;
    }

    public void setDetail(String detail)
    {
        this.detail = detail;
    }

    public Integer getRetryCount()
    {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount)
    {
        this.retryCount = retryCount;
    }
}
