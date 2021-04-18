package com.sercomm.openfire.plugin.cache;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.cache.CacheSizes;
import org.jivesoftware.util.cache.CannotCalculateSizeException;
import org.jivesoftware.util.cache.ExternalizableUtil;

import com.sercomm.openfire.plugin.DeviceModelManager;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.util.ValueUtil;

public class DeviceModelCache implements CacheBase
{
    private boolean isInitialized = false;
    
    private final static String TABLE_S_DEVICE_MODEL = "sDeviceModel";

    private static final String SQL_QUERY_DEVICE_MODEL =
            String.format("SELECT * FROM `%s` WHERE `modelName`=?",
                TABLE_S_DEVICE_MODEL);
    private static final String SQL_UPDATE_DEVICE_MODEL =
            String.format("UPDATE `%s` SET `status`=?,`updatedTime`=?,`script`=? WHERE `modelName`=?",
                TABLE_S_DEVICE_MODEL);

    private String uuid;
    private String modelName;
    private int status;
    private String script;
    private Long creationTime;
    private Long updatedTime;

    public DeviceModelCache()
    {
        this.isInitialized = true;
    }
    
    public DeviceModelCache(String modelName)
    throws SQLException
    {
        this.modelName = modelName;
        
        this.load();
    }
    
    public String getId()
    {
        return this.uuid;
    }
    
    public void setId(String id)
    {
        this.uuid = id;
    }
    
    public String getModelName()
    {
        return this.modelName;
    }
    
    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }
    
    public int getStatus()
    {
        return this.status;
    }
    
    public void setStatus(int status)
    {
        this.status = status;
    }

    public String getScript()
    {
        return this.script;
    }
    
    public void setScript(String script)
    {
        this.script = script;
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

    private void load()
    throws SQLException
    {
        synchronized(this)
        {
            do
            {
                if(true == this.isInitialized)
                {
                    break;
                }
                
                Connection conn = null;
                PreparedStatement stmt = null;
                ResultSet rs = null;
                try
                {
                    conn = DbConnectionManager.getConnection();
                    stmt = conn.prepareStatement(SQL_QUERY_DEVICE_MODEL);
                    
                    int idx = 0;
                    stmt.setString(++idx, this.modelName);
                    
                    rs = stmt.executeQuery();
                    if(!rs.next())
                    {
                        throw new SQLException("DEVICE MODEL CANNOT BE FOUND: " + this.modelName);
                    }
                    
                    this.uuid = rs.getString("uuid");
                    this.modelName = rs.getString("modelName");
                    this.status = rs.getInt("status");
                    this.creationTime = rs.getLong("creationTime");
                    this.updatedTime = rs.getLong("updatedTime");
                    this.script = rs.getString("script");
                    
                    this.isInitialized = true;
                }
                finally
                {
                    DbConnectionManager.closeConnection(rs, stmt, conn);
                }
            }
            while(false);
        }
    }

    public void flush()
    throws DemeterException, Throwable
    {
        synchronized(this)
        {
            // original values
            DeviceModelCache deviceModelCache = DeviceModelManager.getInstance().getDeviceModel(this.modelName);
            
            Connection conn = null;
            PreparedStatement stmt = null;
            do
            {
                // perhaps new values
                if(!ValueUtil.isModified(deviceModelCache.getStatus(), this.status) &&
                   !ValueUtil.isModified(deviceModelCache.getUpdatedTime(), this.updatedTime) &&
                   !ValueUtil.isModified(deviceModelCache.getScript(), this.script))
                {
                    break;
                }
                
                // update database
                try
                {
                    conn = DbConnectionManager.getConnection();
                    stmt = conn.prepareStatement(SQL_UPDATE_DEVICE_MODEL);
                    
                    int idx = 0;
                    stmt.setInt(++idx, this.status);
                    stmt.setLong(++idx, this.updatedTime);
                    stmt.setString(++idx, this.script);
                    stmt.setString(++idx, this.modelName);
                    
                    stmt.executeUpdate();
                }
                finally
                {
                    DbConnectionManager.closeConnection(stmt, conn);
                }
            }
            while(false);
        }
    }

    @Override
    public int getCachedSize() throws CannotCalculateSizeException
    {
        int size = 0;
        // overhead of object
        size += CacheSizes.sizeOfObject();
        // UUID
        size += CacheSizes.sizeOfString(this.uuid);
        // modelName
        size += CacheSizes.sizeOfString(this.modelName);
        // status
        size += CacheSizes.sizeOfInt();
        // creationTime
        size += CacheSizes.sizeOfLong();
        // updatedTime
        size += CacheSizes.sizeOfLong();
        // script
        size += CacheSizes.sizeOfString(this.script);

        return size;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.uuid);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.modelName);
        ExternalizableUtil.getInstance().writeInt(out, this.status);
        ExternalizableUtil.getInstance().writeLong(out, this.creationTime);
        ExternalizableUtil.getInstance().writeLong(out, this.updatedTime);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.script);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.uuid = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.modelName = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.status = ExternalizableUtil.getInstance().readInt(in);
        this.creationTime = ExternalizableUtil.getInstance().readLong(in);
        this.updatedTime = ExternalizableUtil.getInstance().readLong(in);
        this.script = ExternalizableUtil.getInstance().readSafeUTF(in);
    }

    @Override
    public String getUID()
    {
        return this.modelName;
    }
}
