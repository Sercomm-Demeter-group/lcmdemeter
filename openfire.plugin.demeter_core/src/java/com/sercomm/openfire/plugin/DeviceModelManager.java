package com.sercomm.openfire.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.util.DateTime;
import com.sercomm.openfire.plugin.cache.DeviceModelCache;
import com.sercomm.openfire.plugin.exception.DemeterException;

public class DeviceModelManager extends ManagerBase 
{
    private static final String CACHE_NAME = "Demeter Device Model Caches";
    private Cache<String, DeviceModelCache> caches;

    private static final String TABLE_S_DEVICE_MODEL = "sDeviceModel";
    private static final String SQL_INSERT_DEVICE_MODEL =
            String.format("INSERT INTO `%s`(`uuid`,`modelName`,`status`,`creationTime`,`updatedTime`,`script`) VALUES(?,?,?,?,?,?)",
                TABLE_S_DEVICE_MODEL);
    private static final String SQL_QUERY_DEVICE_MODEL_NAME =
            String.format("SELECT `modelName` FROM `%s` WHERE `uuid`=?",
                TABLE_S_DEVICE_MODEL);
    private static final String SQL_DELETE_DEVICE_MODEL =
            String.format("DELETE FROM `%s` WHERE `modelName`=?",
                TABLE_S_DEVICE_MODEL);

    private static class DeviceModelManagerContainer
    {
        private static final DeviceModelManager instance = new DeviceModelManager();
    }

    private DeviceModelManager()
    {
    }
    
    public static DeviceModelManager getInstance()
    {
        return DeviceModelManagerContainer.instance;
    }

    @Override
    protected void onInitialize()
    {
        this.caches = CacheFactory.createCache(CACHE_NAME);
        this.caches.setMaxCacheSize(-1);
        this.caches.setMaxLifetime(86400 * 1000L);
    }

    @Override
    protected void onUninitialize()
    {
    }
    
    private synchronized Lock getLock(String modelName)
    throws DemeterException
    {
        Lock locker = null;
        do
        {
            // ensure that the cache exists
            DeviceModelCache cache = this.getDeviceModel(modelName);
            if(null == cache)
            {
                break;
            }
            
            locker = CacheFactory.getLock(modelName, this.caches);
        }
        while(false);
        
        return locker;
    }

    public DeviceModelCache getDeviceModel(String modelName)
    throws DemeterException
    {
        DeviceModelCache deviceModelCache = this.caches.get(modelName);
        if(null == deviceModelCache)
        {
            try
            {
                deviceModelCache = new DeviceModelCache(modelName);
                
                this.caches.put(modelName, deviceModelCache);
            }
            catch(SQLException e)
            {
                throw new DemeterException(e);
            }
        }

        return deviceModelCache;
    }

    public DeviceModelCache getDeviceModelById(String uuid)
    throws DemeterException
    {
        String modelName = null;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_DEVICE_MODEL_NAME);
            
            int idx = 0;
            stmt.setString(++idx, uuid);
            
            rs = stmt.executeQuery();
            if(!rs.next())
            {
                throw new DemeterException("DEVICE MODEL CANNOT BE FOUND");
            }
            
            modelName = rs.getString("modelName");
        }
        catch(Throwable t)
        {
            throw new DemeterException(t);
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }

        DeviceModelCache deviceModelCache = this.caches.get(modelName);
        if(null == deviceModelCache)
        {
            try
            {
                deviceModelCache = new DeviceModelCache(modelName);
                
                this.caches.put(modelName, deviceModelCache);
            }
            catch(SQLException e)
            {
                throw new DemeterException(e);
            }
        }

        return deviceModelCache;
    }
    
    public DeviceModelCache createDeviceModel(String modelName, String script)
    throws DemeterException
    {
        DeviceModelCache deviceModelCache = null;
        try
        {
            deviceModelCache = this.getDeviceModel(modelName);
        }
        catch(DemeterException ignored) {}

        if(null != deviceModelCache)
        {
            throw new DemeterException("DEVICE MODEL ALREADY EXISTS");
        }
        
        DateTime now = DateTime.now();

        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_INSERT_DEVICE_MODEL);
            
            int idx = 0;
            stmt.setString(++idx, UUID.randomUUID().toString());
            stmt.setString(++idx, modelName);
            stmt.setInt(++idx, 1); // enabled by default
            stmt.setLong(++idx, now.getTimeInMillis());
            stmt.setLong(++idx, now.getTimeInMillis());
            stmt.setString(++idx, script);
            
            stmt.executeUpdate();
            
            deviceModelCache = new DeviceModelCache(modelName);        
            this.caches.put(modelName, deviceModelCache);
        }
        catch(Throwable t)
        {
            throw new DemeterException(t);
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }
        
        return deviceModelCache;
    }

    public DeviceModelCache updateDeviceModel(String modelName, int status, String script)
    throws DemeterException
    {
        DeviceModelCache deviceModelCache = null;

        Lock locker = getLock(modelName);
        try
        {
            locker.lock();
            
            deviceModelCache = this.getDeviceModel(modelName);
            deviceModelCache.setStatus(status);
            deviceModelCache.setUpdatedTime(DateTime.now().getTimeInMillis());
            deviceModelCache.setScript(script);
            deviceModelCache.flush();
            
            this.caches.put(modelName, deviceModelCache);
        }
        catch(Throwable t)
        {
            throw new DemeterException(t);
        }
        finally
        {
            locker.unlock();
        }

        return deviceModelCache;
    }
    
    public DeviceModelCache deleteDeviceModel(String modelName)
    throws DemeterException
    {
        DeviceModelCache deviceModelCache = null;

        Lock locker = getLock(modelName);
        try
        {
            locker.lock();
            
            Connection conn = null;
            PreparedStatement stmt = null;
            try
            {
                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(SQL_DELETE_DEVICE_MODEL);
                
                int idx = 0;
                stmt.setString(++idx, modelName);
                
                stmt.executeUpdate();
            }
            finally
            {
                DbConnectionManager.closeConnection(stmt, conn);
            }
            
            deviceModelCache = this.caches.remove(modelName);
        }
        catch(Throwable t)
        {
            throw new DemeterException(t);
        }
        finally
        {
            locker.unlock();
        }

        return deviceModelCache;
    }
}
