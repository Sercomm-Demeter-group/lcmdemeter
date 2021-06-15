package com.sercomm.openfire.plugin.data.frontend;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Batch 
{
    private String id;
    private String applicationId;
    private String versionId;
    private Integer command;
    private String state;
    private String nodeId;
    private Long creationTime;
    private Long updatedTime;
    private Integer totalCount;
    private Integer doneCount;
    private Integer failedCount;
    private byte[] data;

    public Batch()
    {
    }

    public static Batch from(ResultSet rs)
    throws SQLException
    {
        Batch object = new Batch();

        object.id = rs.getString("id");
        object.applicationId = rs.getString("appId");
        object.versionId = rs.getString("versionId");
        object.command = rs.getInt("command");
        object.state = rs.getString("state");
        object.nodeId = rs.getString("nodeId");
        object.creationTime = rs.getLong("creationTime");
        object.updatedTime = rs.getLong("updatedTime");
        object.totalCount = rs.getInt("totalCount");
        object.doneCount = rs.getInt("doneCount");
        object.failedCount = rs.getInt("failedCount");
        object.data = rs.getBytes("data");

        return object;
    }

    public String getId()
    {
        return this.id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getApplicationId()
    {
        return this.applicationId;
    }

    public void setApplicationId(String applicationId)
    {
        this.applicationId = applicationId;
    }

    public String getVersionId()
    {
        return this.versionId;
    }

    public void setVersionId(String versionId)
    {
        this.versionId = versionId;
    }

    public Integer getCommand()
    {
        return this.command;
    }

    public void setCommand(Integer command)
    {
        this.command = command;
    }

    public String getState()
    {
        return this.state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public String getNodeId()
    {
        return this.nodeId;
    }

    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }

    public Long getCreationTime()
    {
        return this.creationTime;
    }

    public void setCreationTime(Long creationTime)
    {
        this.creationTime = creationTime;
    }

    public Long getUpdatedTime()
    {
        return this.updatedTime;
    }

    public void setUpdatedTime(Long updatedTime)
    {
        this.updatedTime = updatedTime;
    }

    public Integer getTotalCount()
    {
        return this.totalCount;
    }

    public void setTotalCount(Integer totalCount)
    {
        this.totalCount = totalCount;
    }

    public Integer getDoneCount()
    {
        return this.doneCount;
    }

    public void setDoneCount(Integer doneCount)
    {
        this.doneCount = doneCount;
    }

    public Integer getFailedCount()
    {
        return this.failedCount;
    }

    public void setFailedCount(Integer failedCount)
    {
        this.failedCount = failedCount;
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
