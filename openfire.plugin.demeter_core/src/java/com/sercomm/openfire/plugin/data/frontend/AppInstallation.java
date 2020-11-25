package com.sercomm.openfire.plugin.data.frontend;

public class AppInstallation
{
    private String appId;
    private String serial;
    private String mac;
    private String version;
    private String status;
    private Integer executed;
    private Long updatedTime;
    
    public AppInstallation()
    {
    }
    
    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
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

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getExecuted()
    {
        return executed;
    }

    public void setExecuted(Integer executed)
    {
        this.executed = executed;
    }

    public Long getUpdatedTime()
    {
        return updatedTime;
    }

    public void setUpdatedTime(Long updatedTime)
    {
        this.updatedTime = updatedTime;
    }
}
