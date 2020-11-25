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
import org.jivesoftware.util.cache.CacheSizes;
import org.jivesoftware.util.cache.CannotCalculateSizeException;
import org.jivesoftware.util.cache.ExternalizableUtil;

import com.sercomm.commons.util.Log;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.EndUserManager;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.prop.EndUserProperty;
import com.sercomm.openfire.plugin.util.DbConnectionUtil;
import com.sercomm.openfire.plugin.util.ValueUtil;

public class EndUserCache implements CacheBase
{
    private boolean isInitialized = false;
    
    private final static String TABLE_S_END_USER = "sEndUser";
    private final static String TABLE_S_END_USER_PROP = "sEndUserProp";
    
    private final static String SQL_UPDATE_END_USER =
            String.format("UPDATE `%s` SET `role`=?,`storedKey`=?,`encryptedPassword`=?,`valid`=? WHERE `id`=?",
                TABLE_S_END_USER);
    private final static String SQL_QUERY_END_USER =
            String.format("SELECT `id`,`role`,`storedKey`,`encryptedPassword`,`valid`,`creationTime` FROM `%s` WHERE `id`=?",
                TABLE_S_END_USER);
    private static final String SQL_QUERY_PROPERTIES = 
            String.format("SELECT `name`,`propValue` FROM `%s` WHERE `userId`=?", 
                TABLE_S_END_USER_PROP);
    private static final String SQL_DELETE_PROPERTIES =
            String.format("DELETE FROM `%s` WHERE `userId`=?", 
                TABLE_S_END_USER_PROP);
    private static final String SQL_DELETE_PROPERTY =
            String.format("DELETE FROM `%s` WHERE `userId`=? AND `name`=?", 
                TABLE_S_END_USER_PROP);
    private static final String SQL_UPDATE_PROPERTY =
            String.format("INSERT INTO `%s`(`userId`,`name`,`propValue`) VALUES(?,?,?) " +
                "ON DUPLICATE KEY UPDATE `propValue`=?", 
                TABLE_S_END_USER_PROP);

    private String id;
    private EndUserRole endUserRole;
    private String storedKey;
    private String encryptedPassword;
    private Integer valid;
    private Long creationTime;
    private Long updatedTime;
    
    public EndUserCache()
    {
        this.isInitialized = true;
    }
    
    public EndUserCache(String id)
    throws SQLException
    {
        this.id = id;
        this.load();
    }
    
    public String getId()
    {
        return this.id;
    }
    
    public void setId(String id)
    {
        this.id = id;
    }

    public EndUserRole getEndUserRole()
    {
        return this.endUserRole;
    }
    
    public void setEndUserRole(EndUserRole endUserRole)
    {
        this.endUserRole = endUserRole;
    }

    public String getStoredKey()
    {
        return storedKey;
    }

    public void setStoredKey(String storedKey)
    {
        this.storedKey = storedKey;
    }

    public String getEncryptedPassword()
    {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword)
    {
        this.encryptedPassword = encryptedPassword;
    }

    public Integer getValid()
    {
        return this.valid;
    }
    
    public void setValid(Integer valid)
    {
        this.valid = valid;
    }
    
    public Long getCreationTime()
    {
        return creationTime;
    }

    public void setCreationTime(
            Long creationTime)
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
                    stmt = conn.prepareStatement(SQL_QUERY_END_USER);
                    
                    int idx = 0;
                    stmt.setString(++idx, this.id);
                    
                    rs = stmt.executeQuery();
                    if(false == rs.first())
                    {
                        throw new SQLException("USER CANNOT BE FOUND: " + this.id);
                    }
                    
                    this.endUserRole = EndUserRole.fromString(rs.getString("role"));
                    this.storedKey = rs.getString("storedKey");
                    this.encryptedPassword = rs.getString("encryptedPassword");
                    this.valid = rs.getInt("valid");
                    this.creationTime = rs.getLong("creationTime");
                }
                finally
                {
                    DbConnectionManager.closeConnection(rs, stmt, conn);
                }
                
                Map<String,String> properties = null;        
                try
                {
                    properties = new HashMap<String, String>();

                    conn = DbConnectionManager.getConnection();
                    stmt = conn.prepareStatement(SQL_QUERY_PROPERTIES);
                    
                    int idx = 0;
                    stmt.setString(++idx, this.id);
                    
                    rs = stmt.executeQuery();
                    while(rs.next())
                    {
                        properties.put(rs.getString(1), rs.getString(2));
                    }
                }
                finally
                {
                    DbConnectionManager.closeConnection(rs, stmt, conn);
                }
                
                String updatedTimeString = XStringUtil.defaultIfEmpty(
                    properties.remove(EndUserProperty.SERCOMM_ENDUSER_UPDATE_TIME.toString()), XStringUtil.ZERO);
                
                this.updatedTime = Long.parseLong(updatedTimeString);

                // delete the useless property entries which are still in the properties map
                this.deleteProperties(properties);

                this.isInitialized = true;
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
            EndUserCache cache = EndUserManager.getInstance().getUser(this.id);

            Connection conn = null;
            PreparedStatement stmt = null;
            
            do
            {               
                // perhaps new values
                if(ValueUtil.isModified(cache.getEndUserRole().toString(), this.getEndUserRole().toString()) ||
                   ValueUtil.isModified(cache.getStoredKey(), this.getStoredKey()) ||
                   ValueUtil.isModified(cache.getEncryptedPassword(), this.getEncryptedPassword()) ||
                   ValueUtil.isModified(cache.getValid(), this.getValid()))
                {
                    // update database
                    try
                    {
                        conn = DbConnectionManager.getConnection();
                        stmt = conn.prepareStatement(SQL_UPDATE_END_USER);
                        
                        int idx = 0;                
                        stmt.setString(++idx, this.endUserRole.toString());
                        stmt.setString(++idx, this.storedKey);
                        stmt.setString(++idx, this.encryptedPassword);
                        stmt.setInt(++idx, this.valid);
                        stmt.setString(++idx, this.id);
                        
                        stmt.executeUpdate();
                    }
                    catch(Throwable t)
                    {
                        Log.write().error(t.getMessage(), t);
                        break;
                    }
                    finally
                    {
                        DbConnectionManager.closeConnection(stmt, conn);
                    }
                }
                
                // perhaps new values
                Map<String, String> properties = new HashMap<String, String>();
                if(ValueUtil.isModified(cache.getUpdatedTime(), this.getUpdatedTime()))
                {
                    properties.put(EndUserProperty.SERCOMM_ENDUSER_UPDATE_TIME.toString(), this.getUpdatedTime().toString());
                }

                if(properties.isEmpty())
                {
                    break;
                }

                // update database
                boolean abort = false;
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
                        stmt.setString(++idx, this.id);
                        stmt.setString(++idx, property.getKey());
                        stmt.setString(++idx, property.getValue());
                        stmt.setString(++idx, property.getValue());
                        
                        stmt.addBatch();                
                    }
                    
                    stmt.executeBatch();
                }
                catch(Throwable t)
                {
                    abort = true;
                    Log.write().error(t.getMessage(), t);
                }
                finally
                {
                    DbConnectionManager.closeStatement(stmt);
                    DbConnectionUtil.closeTransaction(conn, abort);
                    DbConnectionManager.closeConnection(conn);
                }
            }
            while(false);
        }
    }
    
    @Override
    public int getCachedSize()
    throws CannotCalculateSizeException
    {
        int size = 0;
        // overhead of object
        size += CacheSizes.sizeOfObject();
        // name
        size += CacheSizes.sizeOfString(this.id);
        // endUserRole
        size += CacheSizes.sizeOfString(this.endUserRole == null ? XStringUtil.BLANK : this.endUserRole.toString());
        // storedKey
        size += CacheSizes.sizeOfString(this.storedKey);
        // encryptedPassword
        size += CacheSizes.sizeOfString(this.encryptedPassword);
        // valid
        size += CacheSizes.sizeOfInt();
        // creationTime
        size += CacheSizes.sizeOfLong();
        // updatedTime
        size += CacheSizes.sizeOfLong();
        
        return size;
    }

    @Override
    public void writeExternal(
            ObjectOutput out)
    throws IOException
    {
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.id);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.endUserRole == null ? XStringUtil.BLANK : this.endUserRole.toString());
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.storedKey);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.encryptedPassword);
        ExternalizableUtil.getInstance().writeInt(out, this.valid);
        ExternalizableUtil.getInstance().writeLong(out, this.creationTime);
        ExternalizableUtil.getInstance().writeLong(out, this.updatedTime);
    }

    @Override
    public void readExternal(
            ObjectInput in)
    throws IOException, ClassNotFoundException
    {
        this.id = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.endUserRole = EndUserRole.fromString(ExternalizableUtil.getInstance().readSafeUTF(in));
        this.storedKey = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.encryptedPassword = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.valid = ExternalizableUtil.getInstance().readInt(in);
        this.creationTime = ExternalizableUtil.getInstance().readLong(in);
        this.updatedTime = ExternalizableUtil.getInstance().readLong(in);
    }

    @Override
    public String getUID()
    {
        return this.id;
    }

    public void deleteProperties() 
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        try 
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_DELETE_PROPERTIES);
            stmt.setString(1, this.id);
            
            stmt.executeUpdate();
        }
        catch (SQLException e) 
        {
            Log.write().error(e.getMessage(), e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }
    }
    
    private void deleteProperties(Map<String, String> properties)
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
                stmt.setString(++idx, this.id);
                stmt.setString(++idx, property.getKey());
                
                stmt.addBatch();                
            }
            
            stmt.executeBatch();
        }
        catch(Throwable t) 
        {
            Log.write().error(t.getMessage(), t);
            abort = true;
        }
        finally 
        {
            DbConnectionManager.closeStatement(stmt);
            DbConnectionUtil.closeTransaction(conn, abort);
            DbConnectionManager.closeConnection(conn);
        }
    }
}
