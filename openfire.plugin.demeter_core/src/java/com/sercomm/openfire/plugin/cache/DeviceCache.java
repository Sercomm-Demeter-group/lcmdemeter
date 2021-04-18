package com.sercomm.openfire.plugin.cache;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.cache.CacheSizes;
import org.jivesoftware.util.cache.CannotCalculateSizeException;
import org.jivesoftware.util.cache.ExternalizableUtil;

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.define.DeviceState;
import com.sercomm.openfire.plugin.define.DeviceType;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.prop.DeviceProperty;
import com.sercomm.openfire.plugin.util.DbConnectionUtil;
import com.sercomm.openfire.plugin.util.ValueUtil;

public class DeviceCache implements CacheBase
{
    private boolean isInitialized = false;

    private final static String TABLE_S_DEVICE_PROP = "sDeviceProp";
    
    private final static String SQL_QUERY_PROPERTIES = 
            String.format("SELECT `name`,`propValue` FROM `%s` WHERE `serial`=? AND `mac`=?", 
                TABLE_S_DEVICE_PROP);
    //private static final String SQL_LOAD_PROPERTY =
    //        String.format("SELECT `propValue` FROM `%s` WHERE `serial`=? AND `mac`=? AND `name`=?", 
    //            TABLE_S_DEVICE_PROP);
    private final static String SQL_DELETE_PROPERTIES =
            String.format("DELETE FROM `%s` WHERE `serial`=? AND `mac`=?", 
                TABLE_S_DEVICE_PROP);
    private final static String SQL_DELETE_PROPERTY =
            String.format("DELETE FROM `%s` WHERE `serial`=? AND `mac`=? AND `name`=?", 
                TABLE_S_DEVICE_PROP);
    private final static String SQL_UPDATE_PROPERTY =
            String.format("INSERT INTO `%s`(`serial`,`mac`,`name`,`propValue`) VALUES(?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE `propValue`=?", 
                TABLE_S_DEVICE_PROP);

    private String serial;
    private String mac;
    private String platform;
    private DeviceType deviceType;
    private String modelName;
    private String firmwareVersion;
    private DeviceState deviceState;
    private Long lastOnlineTime;
    private Long lastOfflineTime;
    private String company;
    private String customName;
    private Boolean enable;
    private Integer protocolVersion;
    private Long creationTime;
    
    public DeviceCache()
    {
        this.isInitialized = true;
    }
    
    public DeviceCache(String serial, String mac)
    throws SQLException, UserNotFoundException
    {
        this.serial = serial;
        this.mac = mac;
        
        this.loadProperties();
    }
    
    public String getSerial()
    {
        return serial;
    }

    public String getMac()
    {
        return mac;
    }

    public String getPlatform()
    {
        return this.platform;
    }
    
    public void setPlatform(String platform)
    {
        this.platform = platform;
    }
    
    public DeviceType getDeviceType()
    {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType)
    {
        this.deviceType = deviceType;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getFirmwareVersion()
    {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion)
    {
        this.firmwareVersion = firmwareVersion;
    }

    public DeviceState getDeviceState()
    {
        return deviceState;
    }

    public void setDeviceState(DeviceState deviceState)
    {
        this.deviceState = deviceState;
    }

    public Long getLastOnlineTime()
    {
        return lastOnlineTime;
    }

    public void setLastOnlineTime(
            Long lastOnlineTime)
    {
        this.lastOnlineTime = lastOnlineTime;
    }

    public Long getLastOfflineTime()
    {
        return lastOfflineTime;
    }

    public void setLastOfflineTime(
            Long lastOfflineTime)
    {
        this.lastOfflineTime = lastOfflineTime;
    }

    public String getCompany()
    {
        return this.company;
    }
    
    public void setCompany(String company)
    {
        this.company = company;
    }
    
    public String getCustomName()
    {
        return this.customName;
    }
    
    public void setCustomName(String customName)
    {
        this.customName = customName;
    }
    
    public Boolean getEnable()
    {
        return this.enable;
    }
    
    public void setEnable(Boolean enable)
    {
        this.enable = enable;
    }
    
    public Integer getProtocolVersion()
    {
        return this.protocolVersion;
    }
    
    public void setProtocolVersion(Integer protocolVersion)
    {
        this.protocolVersion = protocolVersion;
    }
    
    public Long getCreationTime()
    {
        return this.creationTime;
    }
    
    public void setCreationTime(Long creationTime)
    {
        this.creationTime = creationTime;
    }
    
    public void flush()
    throws DemeterException, UserNotFoundException
    {
        synchronized (this) 
        {
            // original values
            DeviceCache cache = DeviceManager.getInstance().getDeviceCache(this.serial, this.mac);
            
            // perhaps new values
            Map<String, String> properties = new HashMap<String, String>();

            if(ValueUtil.isModified(
                null != cache.deviceType ? cache.deviceType.name() : XStringUtil.BLANK, 
                null != this.deviceType ? this.deviceType.name() : XStringUtil.BLANK))
            {
                properties.put(DeviceProperty.SERCOMM_DEVICE_TYPE.toString(), this.deviceType.name());
            }

            if(ValueUtil.isModified(cache.modelName, this.modelName))
            {
                properties.put(DeviceProperty.SERCOMM_DEVICE_MODEL_NAME.toString(), this.modelName);
            }

            if(ValueUtil.isModified(cache.firmwareVersion, this.firmwareVersion))
            {
                properties.put(DeviceProperty.SERCOMM_DEVICE_FIRMWARE_VERSION.toString(), this.firmwareVersion);
            }

            if(ValueUtil.isModified(
                null != cache.deviceState ? cache.deviceState.name() : XStringUtil.BLANK, 
                null != this.deviceState ? this.deviceState.name() : XStringUtil.BLANK))
            {
                properties.put(DeviceProperty.SERCOMM_DEVICE_STATE.toString(), this.deviceState.name());
            }

            if(ValueUtil.isModified(cache.lastOfflineTime, this.lastOfflineTime))
            {
                properties.put(DeviceProperty.SERCOMM_DEVICE_LAST_OFFLINE_TIME.toString(), this.lastOfflineTime.toString());
            }

            if(ValueUtil.isModified(cache.lastOnlineTime, this.lastOnlineTime))
            {
                properties.put(DeviceProperty.SERCOMM_DEVICE_LAST_ONLINE_TIME.toString(), this.lastOnlineTime.toString());
            }

            if(ValueUtil.isModified(cache.company, this.company))
            {
                properties.put(DeviceProperty.SERCOMM_DEVICE_COMPANY.toString(), this.company);
            }

            if(ValueUtil.isModified(cache.customName, this.customName))
            {
                properties.put(DeviceProperty.SERCOMM_DEVICE_CUSTOM_NAME.toString(), this.customName);
            }

            if(ValueUtil.isModified(cache.enable, this.enable))
            {
                properties.put(DeviceProperty.SERCOMM_DEVICE_ENABLE.toString(), this.enable.toString());
            }

            if(ValueUtil.isModified(cache.protocolVersion, this.protocolVersion))
            {
                properties.put(DeviceProperty.SERCOMM_DEVICE_PROTOCOL_VERSION.toString(), this.protocolVersion.toString());
            }

            // update database
            try
            {
                this.updateProperties(properties);
            }
            catch(SQLException e)
            {
                throw new DemeterException(e);
            }

            // update cluster caches
            DeviceManager.getInstance().updateDeviceCache(this.serial, this.mac, this);
        }        
    }
    
    private void loadProperties() 
    throws SQLException, UserNotFoundException
    {
        synchronized (this) 
        {
            do
            {
                if(true == this.isInitialized)
                {
                    break;
                }
                
                final String nodeName = NameRule.formatDeviceName(this.serial, this.mac);
                // leverage Openfire user management
                User device = UserManager.getInstance().getUser(nodeName);
                final Long creationTime = device.getCreationDate().getTime();
                
                Connection conn = null;
                PreparedStatement stmt = null;
                ResultSet rs = null;
    
                Map<String,String> properties = null;        
                try
                {
                    properties = new HashMap<String, String>();
                    
                    conn = DbConnectionManager.getConnection();
                    stmt = conn.prepareStatement(SQL_QUERY_PROPERTIES);
                    
                    int idx = 0;
                    stmt.setString(++idx, this.serial);
                    stmt.setString(++idx, this.mac);

                    rs = stmt.executeQuery();
                    while (rs.next()) 
                    {
                        properties.put(rs.getString(1), rs.getString(2));
                    }
                }
                finally
                {
                    DbConnectionManager.closeConnection(rs, stmt, conn);
                }

                String platformString = XStringUtil.defaultIfEmpty(
                    properties.remove(DeviceProperty.SERCOMM_DEVICE_PLATFORM.toString()), XStringUtil.BLANK);
                String deviceTypeString = XStringUtil.defaultIfEmpty(
                    properties.remove(DeviceProperty.SERCOMM_DEVICE_TYPE.toString()), XStringUtil.BLANK);
                String modelNameString = XStringUtil.defaultIfEmpty(
                    properties.remove(DeviceProperty.SERCOMM_DEVICE_MODEL_NAME.toString()), XStringUtil.BLANK);
                String firmwareVersionString = XStringUtil.defaultIfEmpty(
                    properties.remove(DeviceProperty.SERCOMM_DEVICE_FIRMWARE_VERSION.toString()), XStringUtil.BLANK);
                String deviceStateString = XStringUtil.defaultIfEmpty(
                    properties.remove(DeviceProperty.SERCOMM_DEVICE_STATE.toString()), DeviceState.OFFLINE.name());
                String lastOfflineString = XStringUtil.defaultIfEmpty(
                    properties.remove(DeviceProperty.SERCOMM_DEVICE_LAST_OFFLINE_TIME.toString()), XStringUtil.ZERO);
                String lastOnlineString = XStringUtil.defaultIfEmpty(
                    properties.remove(DeviceProperty.SERCOMM_DEVICE_LAST_ONLINE_TIME.toString()), XStringUtil.ZERO);
                String companyString = XStringUtil.defaultIfEmpty(
                    properties.remove(DeviceProperty.SERCOMM_DEVICE_COMPANY.toString()), "SERCOMM");
                String customNameString = XStringUtil.defaultIfEmpty(
                    properties.remove(DeviceProperty.SERCOMM_DEVICE_CUSTOM_NAME.toString()), this.serial);
                String enableString = XStringUtil.defaultIfEmpty(
                    properties.remove(DeviceProperty.SERCOMM_DEVICE_ENABLE.toString()), Boolean.TRUE.toString());
                String protocolVersionString = XStringUtil.defaultIfEmpty(
                    properties.remove(DeviceProperty.SERCOMM_DEVICE_PROTOCOL_VERSION.toString()), Integer.toString(0));
                
                this.platform = platformString;
                this.deviceType = DeviceType.fromString(deviceTypeString);
                this.modelName = modelNameString;
                this.firmwareVersion = firmwareVersionString;
                this.deviceState = DeviceState.fromString(deviceStateString);
                this.lastOfflineTime = Long.parseLong(lastOfflineString);
                this.lastOnlineTime = Long.parseLong(lastOnlineString);
                this.company = companyString;
                this.customName = customNameString;
                this.enable = Boolean.valueOf(enableString);
                this.protocolVersion = Integer.parseInt(protocolVersionString);
                this.creationTime = creationTime;
                
                // delete the useless property entries which are still in the properties map
                this.deleteProperties(properties);

                this.isInitialized = true;
            }
            while(false);
         }        
    }

    private void updateProperties(Map<String, String> properties)
    throws SQLException
    {
        if(true == properties.isEmpty())
        {
            return;
        }
        
        boolean abort = false;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        try 
        {
            conn = DbConnectionManager.getConnection();
            conn = DbConnectionUtil.openTransaction(conn);

            stmt = conn.prepareStatement(SQL_UPDATE_PROPERTY);            
            Iterator<Map.Entry<String, String>> iterator = properties.entrySet().iterator();
            while(true == iterator.hasNext())
            {
                Map.Entry<String, String> property = iterator.next();
                
                int idx = 0;
                stmt.setString(++idx, this.serial);
                stmt.setString(++idx, this.mac);
                stmt.setString(++idx, property.getKey());
                stmt.setString(++idx, property.getValue());
                stmt.setString(++idx, property.getValue());
                
                stmt.addBatch();                
            }
            
            stmt.executeBatch();            
        }
        catch(SQLException e) 
        {
            abort = true;
            throw e;
        }
        finally 
        {
            DbConnectionManager.closeStatement(stmt);
            DbConnectionUtil.closeTransaction(conn, abort);
            DbConnectionManager.closeConnection(conn);
        }
    }

    public void deleteProperties()
    throws SQLException
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        try 
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_DELETE_PROPERTIES);
            stmt.setString(1, this.serial);
            stmt.setString(2, this.mac);
            
            stmt.executeUpdate();
        }
        finally 
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }
    }

    private void deleteProperties(Map<String, String> properties)
    throws SQLException
    {
        if(true == properties.isEmpty())
        {
            return;
        }
        
        boolean abort = false;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        try 
        {
            conn = DbConnectionManager.getConnection();
            conn = DbConnectionUtil.openTransaction(conn);

            stmt = conn.prepareStatement(SQL_DELETE_PROPERTY);
            Iterator<Map.Entry<String, String>> iterator = properties.entrySet().iterator();
            while(true == iterator.hasNext())
            {
                Map.Entry<String, String> property = iterator.next();
                
                int idx = 0;
                stmt.setString(++idx, this.serial);
                stmt.setString(++idx, this.mac);
                stmt.setString(++idx, property.getKey());
                
                stmt.addBatch();                
            }
            
            stmt.executeBatch();
        }
        catch(SQLException e) 
        {
            abort = true;
            throw e;
        }
        finally 
        {
            DbConnectionManager.closeStatement(stmt);
            DbConnectionUtil.closeTransaction(conn, abort);
            DbConnectionManager.closeConnection(conn);
        }
    }

    @Override
    public int getCachedSize()
    throws CannotCalculateSizeException
    {
        int size = 0;
        // overhead of object
        size += CacheSizes.sizeOfObject();
        // serial
        size += CacheSizes.sizeOfString(this.serial);
        // mac
        size += CacheSizes.sizeOfString(this.mac);
        // platform
        size += CacheSizes.sizeOfString(this.platform);
        // deviceType
        size += CacheSizes.sizeOfString(null == this.deviceType ? XStringUtil.BLANK : this.deviceType.name());
        // modelName
        size += CacheSizes.sizeOfString(this.modelName);
        // firmwareVersion
        size += CacheSizes.sizeOfString(this.firmwareVersion);
        // deviceState
        size += CacheSizes.sizeOfString(null == this.deviceState ? XStringUtil.BLANK : this.deviceState.name());
        // lastOfflineTime
        size += CacheSizes.sizeOfLong();
        // lastOnlineTime
        size += CacheSizes.sizeOfLong();
        // company
        size += CacheSizes.sizeOfString(this.company);
        // customName
        size += CacheSizes.sizeOfString(this.customName);
        // enable
        size += CacheSizes.sizeOfBoolean();
        // protocolVersion
        size += CacheSizes.sizeOfInt();
        // creationTime
        size += CacheSizes.sizeOfLong();
        
        return size;
    }

    @Override
    public void writeExternal(
            ObjectOutput out)
    throws IOException
    {
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.serial);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.mac);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.platform);
        ExternalizableUtil.getInstance().writeSafeUTF(out, null == this.deviceType ? XStringUtil.BLANK : this.deviceType.name());
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.modelName);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.firmwareVersion);
        ExternalizableUtil.getInstance().writeSafeUTF(out, null == this.deviceState ? XStringUtil.BLANK : this.deviceState.name());
        ExternalizableUtil.getInstance().writeLong(out, this.lastOfflineTime);
        ExternalizableUtil.getInstance().writeLong(out, this.lastOnlineTime);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.company);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.customName);
        ExternalizableUtil.getInstance().writeBoolean(out, this.enable);
        ExternalizableUtil.getInstance().writeInt(out, this.protocolVersion);
        ExternalizableUtil.getInstance().writeLong(out, this.creationTime);
    }

    @Override
    public void readExternal(
            ObjectInput in)
    throws IOException, ClassNotFoundException
    {
        this.serial = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.mac = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.platform = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.deviceType = DeviceType.fromString(ExternalizableUtil.getInstance().readSafeUTF(in));
        this.modelName = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.firmwareVersion = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.deviceState = DeviceState.fromString(ExternalizableUtil.getInstance().readSafeUTF(in));
        this.lastOfflineTime = ExternalizableUtil.getInstance().readLong(in);
        this.lastOnlineTime = ExternalizableUtil.getInstance().readLong(in);
        this.company = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.customName = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.enable = ExternalizableUtil.getInstance().readBoolean(in);
        this.protocolVersion = ExternalizableUtil.getInstance().readInt(in);
        this.creationTime = ExternalizableUtil.getInstance().readLong(in);
    }

    @Override
    public String getUID()
    {
        return NameRule.formatDeviceName(this.serial, this.mac);
    }
}
