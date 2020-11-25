package com.sercomm.openfire.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.id.NameRule;
import com.sercomm.openfire.plugin.cache.OwnershipCache;
import com.sercomm.openfire.plugin.data.frontend.Ownership;
import com.sercomm.openfire.plugin.define.OwnershipType;
import com.sercomm.openfire.plugin.exception.DemeterException;

public class OwnershipManager extends ManagerBase 
{
    private final static String CACHE_NAME_OWNS = "Demeter End User Owns Caches";
    private final static String CACHE_NAME_OWNED = "Demeter Device Owned Caches";

    // pair<deviceId/nodeName, pair<userId, ownerships>>
    private Cache<String, ConcurrentHashMap<String, OwnershipCache>> ownedCaches; 
    // pair<userId, pair<deviceId/nodeName, ownerships>>
    private Cache<String, ConcurrentHashMap<String, OwnershipCache>> ownsCaches;

    private final static String TABLE_S_OWNERSHIP = "sOwnership";
    private final static String SQL_QUERY_OWNED_OWNERSHIPS =
            String.format("SELECT * FROM `%s` WHERE `serial`=? AND `mac`=?",
                TABLE_S_OWNERSHIP);
    private final static String SQL_QUERY_OWNS_OWNERSHIPS =
            String.format("SELECT * FROM `%s` WHERE `userId`=?",
                TABLE_S_OWNERSHIP);
    private final static String SQL_UPDATE_OWNERSHIP =
            String.format("INSERT INTO `%s`(`serial`,`mac`,`userId`,`type`,`creationTime`) VALUES(?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE `type`=?",
                TABLE_S_OWNERSHIP);
    private final static String SQL_DELETE_OWNERSHIP =
            String.format("DELETE FROM `%s` WHERE `serial`=? AND `mac`=? AND `userId`=?",
                TABLE_S_OWNERSHIP);
    
    private static class OwnershipManagerContainer
    {
        private final static OwnershipManager instance = new OwnershipManager();
    }
    
    private OwnershipManager()
    {
    }

    public static OwnershipManager getInstance()
    {
        return OwnershipManagerContainer.instance;
    }
    
    @SuppressWarnings("all")
    @Override
    protected void onInitialize()
    {
        this.ownsCaches = CacheFactory.createCache(CACHE_NAME_OWNS);
        this.ownsCaches.setMaxCacheSize(-1);
        this.ownsCaches.setMaxLifetime(60 * 60 * 1000L);

        this.ownedCaches = CacheFactory.createCache(CACHE_NAME_OWNED);
        this.ownedCaches.setMaxCacheSize(-1);
        this.ownedCaches.setMaxLifetime(60 * 60 * 1000L);
    }

    @Override
    protected void onUninitialize()
    {
    }
    
    public Map<String, OwnershipCache> getOwnerships(String serial, String mac)
    throws DemeterException, Throwable
    {        
        final String nodeName = NameRule.formatDeviceName(serial, mac);
        ConcurrentHashMap<String, OwnershipCache> ownerships = this.ownedCaches.get(nodeName);
        if(null == ownerships)
        {
            ownerships = new ConcurrentHashMap<String, OwnershipCache>();
            
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try
            {
                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(SQL_QUERY_OWNED_OWNERSHIPS);
                
                int idx = 0;
                stmt.setString(++idx, serial);
                stmt.setString(++idx, mac);
                
                rs = stmt.executeQuery();
                while(rs.next())
                {
                    Ownership object = Ownership.from(rs);
                    final String userId = object.getUserId();
                    
                    OwnershipCache cache = new OwnershipCache();
                    cache.setSerial(serial);
                    cache.setMac(mac);
                    cache.setUserId(userId);
                    cache.setOwnershipType(OwnershipType.fromString(object.getType()));
                    cache.setCreationTime(object.getCreationTime());
                    
                    ownerships.put(userId, cache);
                }
                
                this.ownedCaches.put(nodeName, ownerships);
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, stmt, conn);
            }
        }
        
        return ownerships;
    }
    
    public Map<String, OwnershipCache> getOwnerships(String userId)
    throws DemeterException, Throwable
    {
        if(false == EndUserManager.getInstance().isRegisteredUser(userId))
        {
            throw new DemeterException("END USER DOES NOT EXIST");
        }
        
        ConcurrentHashMap<String, OwnershipCache> ownerships = this.ownsCaches.get(userId);
        if(null == ownerships)
        {
            synchronized(userId.intern())
            {
                ownerships = new ConcurrentHashMap<String, OwnershipCache>();
                
                Connection conn = null;
                PreparedStatement stmt = null;
                ResultSet rs = null;
                try
                {
                    conn = DbConnectionManager.getConnection();
                    stmt = conn.prepareStatement(SQL_QUERY_OWNS_OWNERSHIPS);
                    
                    int idx = 0;
                    stmt.setString(++idx, userId);
                    
                    rs = stmt.executeQuery();
                    while(rs.next())
                    {
                        Ownership object = Ownership.from(rs);
                        final String serial = object.getSerial();
                        final String mac = object.getMac();
                        final String nodeName = NameRule.formatDeviceName(serial, mac);
                        
                        OwnershipCache cache = new OwnershipCache();
                        cache.setSerial(serial);
                        cache.setMac(mac);
                        cache.setUserId(userId);
                        cache.setOwnershipType(OwnershipType.fromString(object.getType()));
                        cache.setCreationTime(object.getCreationTime());
                        
                        ownerships.put(nodeName, cache);
                    }
                    
                    this.ownsCaches.put(userId, ownerships);
                }
                finally
                {
                    DbConnectionManager.closeConnection(rs, stmt, conn);
                }
            }
        }

        return ownerships;
    }
    
    public void updateOwnership(
            String serial,
            String mac,
            String userId,
            OwnershipType ownershipType)
    throws DemeterException, Throwable
    {
        final String nodeName = NameRule.formatDeviceName(serial, mac);
        if(null != ownershipType)
        {
            // insert/update the ownership
            synchronized(nodeName.intern())
            {
                final Long creationTime = System.currentTimeMillis();
                
                // update database
                Connection conn = null;
                PreparedStatement stmt = null;
                try
                {
                    conn = DbConnectionManager.getConnection();
                    stmt = conn.prepareStatement(SQL_UPDATE_OWNERSHIP);
                    
                    int idx = 0;
                    stmt.setString(++idx, serial);
                    stmt.setString(++idx, mac);
                    stmt.setString(++idx, userId);
                    stmt.setString(++idx, ownershipType.name());
                    stmt.setLong(++idx, creationTime);
                    stmt.setString(++idx, ownershipType.name());
                    
                    stmt.executeUpdate();
                }
                finally
                {
                    DbConnectionManager.closeConnection(stmt, conn);
                }
                
                // update caches
                ConcurrentHashMap<String, OwnershipCache> collection;
                collection = this.ownedCaches.get(nodeName);
                if(null != collection)
                {
                    OwnershipCache cache = collection.get(userId);
                    if(null != cache)
                    {
                        cache.setOwnershipType(ownershipType);
                    }
                    else
                    {
                        cache = new OwnershipCache();
                        cache.setSerial(serial);
                        cache.setMac(mac);
                        cache.setUserId(userId);
                        cache.setOwnershipType(ownershipType);
                        cache.setCreationTime(creationTime);
                        
                        collection.put(userId, cache);
                    }
                    
                    this.ownedCaches.put(nodeName, collection);
                }
                
                collection = this.ownsCaches.get(userId);
                if(null != collection)
                {
                    OwnershipCache cache = collection.get(nodeName);
                    if(null != cache)
                    {
                        cache.setOwnershipType(ownershipType);
                    }
                    else
                    {
                        cache = new OwnershipCache();
                        cache.setSerial(serial);
                        cache.setMac(mac);
                        cache.setUserId(userId);
                        cache.setOwnershipType(ownershipType);
                        cache.setCreationTime(creationTime);
                        
                        collection.put(nodeName, cache);
                    }
                    this.ownsCaches.put(userId, collection);
                }
            }
        }
        else
        {
            // delete the ownership if exists
            synchronized(nodeName.intern())
            {
                // update database
                Connection conn = null;
                PreparedStatement stmt = null;
                try
                {
                    conn = DbConnectionManager.getConnection();
                    stmt = conn.prepareStatement(SQL_DELETE_OWNERSHIP);
                    
                    int idx = 0;
                    stmt.setString(++idx, serial);
                    stmt.setString(++idx, mac);
                    stmt.setString(++idx, userId);
                    
                    stmt.executeUpdate();
                }
                finally
                {
                    DbConnectionManager.closeConnection(stmt, conn);
                }
                
                // update caches
                ConcurrentHashMap<String, OwnershipCache> collection;
                collection = this.ownedCaches.get(nodeName);
                if(null != collection)
                {
                    if(collection.containsKey(userId))
                    {
                        collection.remove(userId);
                        this.ownedCaches.put(nodeName, collection);
                    }                    
                }
                
                collection = this.ownsCaches.get(userId);
                if(null != collection)
                {
                    if(collection.containsKey(nodeName))
                    {
                        collection.remove(nodeName);
                        this.ownsCaches.put(userId, collection);
                    }
                }
            }
        }
    }
}
