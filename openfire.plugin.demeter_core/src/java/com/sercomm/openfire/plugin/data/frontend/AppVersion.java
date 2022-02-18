package com.sercomm.openfire.plugin.data.frontend;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AppVersion
{
    private String id;
    private String appId;
    private String version;
    private Integer status;
    private String filename;
    private Long installedCount;
    private Long removedCount;
    private Long creationTime;
    private String ipkFilePath;
    private Long ipkFileSize;
    private String releaseNote;
    private String realVersion;

    public AppVersion()
    {
    }
    
    public static AppVersion from(ResultSet rs)
    throws SQLException
    {
        AppVersion object = new AppVersion();
        
        object.id = rs.getString("id");
        object.appId = rs.getString("appId");
        object.version = rs.getString("version");
        object.status = rs.getInt("status");
        object.filename = rs.getString("filename");
        object.installedCount = rs.getLong("installedCount");
        object.removedCount = rs.getLong("removedCount");
        object.creationTime = rs.getLong("creationTime");
        object.ipkFilePath = rs.getString("ipkFilePath");
        object.ipkFileSize = rs.getLong("ipkFileSize");
        object.releaseNote = rs.getString("releaseNote");
        object.realVersion = rs.getString("realVersion");

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

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public Integer getStatus()
    {
        return this.status;
    }
    
    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public String getFilename()
    {
        return this.filename;
    }
    
    public void setFilename(String filename)
    {
        this.filename = filename;
    }
    
    public Long getInstalledCount()
    {
        return installedCount;
    }

    public void setInstalledCount(Long installedCount)
    {
        this.installedCount = installedCount;
    }

    public Long getRemovedCount()
    {
        return removedCount;
    }

    public void setRemovedCount(Long removedCount)
    {
        this.removedCount = removedCount;
    }

    public Long getCreationTime()
    {
        return creationTime;
    }

    public void setCreationTime(Long creationTime)
    {
        this.creationTime = creationTime;
    }

    public String getIPKFilePath()
    {
        return ipkFilePath;
    }

    public void setIPKFilePath(String ipkFilePath)
    {
        this.ipkFilePath = ipkFilePath;
    }

    public Long getIPKFileSize()
    {
        return ipkFileSize;
    }

    public void setIPKFileSize(Long ipkFileSize)
    {
        this.ipkFileSize = ipkFileSize;
    }

    public String getReleaseNote()
    {
        return releaseNote;
    }

    public void setReleaseNote(String releaseNote)
    {
        this.releaseNote = releaseNote;
    }
    public String getRealVersion()
    {
        return realVersion;
    }

    public void setRealVersion(String realVersion)
    {
        this.realVersion = realVersion;
    }
}
