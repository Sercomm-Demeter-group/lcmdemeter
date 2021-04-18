package com.sercomm.openfire.plugin;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.common.util.Algorithm;
import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.cache.EndUserCache;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;

public class EndUserManager extends ManagerBase 
{
    private static final Logger log = LoggerFactory.getLogger(EndUserManager.class);

    private final static String CACHE_NAME = "Demeter End User Caches";
    private Cache<String, EndUserCache> caches;
    
    private final static String SECRET_KEY_SPEC = "AES";
    private final static int SECRET_KEY_LENGTH = 256;
    
    private final static String TABLE_S_END_USER = "sEndUser";
    private final static String SQL_INSERT_END_USER =
            String.format("INSERT INTO `%s`(`id`,`uuid`,`role`,`storedKey`,`encryptedPassword`,`valid`,`creationTime`) VALUES(?,?,?,?,?,?,?)",
                TABLE_S_END_USER);
    private final static String SQL_DELETE_END_USER =
            String.format("DELETE FROM `%s` WHERE `id`=?",
                TABLE_S_END_USER);
    private final static String SQL_QUERY_END_USER_NAME =
            String.format("SELECT `id` FROM `%s` WHERE `uuid`=?",
                TABLE_S_END_USER);
    private final static String SQL_QUERY_END_USERS =
            String.format("SELECT `id` FROM `%s`",
                TABLE_S_END_USER);

    private static class EndUserManagerContainer
    {
        private final static EndUserManager instance = new EndUserManager();
    }

    private EndUserManager()
    {
    }

    public static EndUserManager getInstance()
    {
        return EndUserManagerContainer.instance;
    }
    
    @SuppressWarnings("all")
    @Override
    protected void onInitialize()
    {
        this.caches = CacheFactory.createCache(CACHE_NAME);
        this.caches.setMaxCacheSize(-1);
        this.caches.setMaxLifetime(1 * 60 * 60 * 1000L); // 1 hour
        
        try
        {
            // create default administrator account if not exists
            if(false == this.isRegisteredUser("admin"))
            {
                this.createUser("admin", "12345678", EndUserRole.ADMIN);
            }
        }
        catch(Throwable t)
        {
            log.error(t.getMessage(), t);
        }
    }

    @Override
    protected void onUninitialize()
    {
    }

    public boolean isRegisteredUserById(String uuid)
    throws DemeterException, Throwable
    {
        EndUserCache cache;
        try
        {
            cache = this.getUserById(uuid);
        }
        catch(Throwable ignored) { return false; }
        
        if(null != cache)
        {
            return true;
        }
        
        return false;
    }

    public boolean isRegisteredUser(String name)
    throws DemeterException, Throwable
    {
        EndUserCache cache;
        try
        {
            cache = this.getUser(name);
        }
        catch(Throwable ignored) { return false; }
        
        if(null != cache)
        {
            return true;
        }
        
        return false;
    }
    
    public EndUserCache getUser(String name)
    throws DemeterException, Throwable
    {
        EndUserCache cache = null;
        do
        {
            if(true == this.caches.containsKey(name))
            {
                cache = this.caches.get(name);
                break;
            }

            try
            {
                cache = new EndUserCache(name);
            }
            catch(SQLException e)
            {
                throw new DemeterException(e.getMessage());
            }

            this.caches.put(name, cache);
        }
        while(false);

        return cache;
    }
    
    public EndUserCache getUserById(String uuid)
    throws DemeterException, Throwable
    {
        EndUserCache cache = null;
        do
        {
            String name = this.queryEndUserName(uuid);
            if(XStringUtil.isBlank(name))
            {
                throw new DemeterException("ID CANNOT BE FOUND");
            }

            try
            {
                if(true == this.caches.containsKey(name))
                {
                    cache = this.caches.get(name);
                    break;
                }

                cache = new EndUserCache(name);
            }
            catch(Throwable t)
            {
                throw new DemeterException(t.getMessage());
            }

            this.caches.put(name, cache);
        }
        while(false);

        return cache;
    }

    public List<EndUserCache> getUsers()
    throws DemeterException, Throwable
    {
        List<EndUserCache> users = new ArrayList<EndUserCache>();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_END_USERS);
            rs = stmt.executeQuery();

            while(rs.next())
            {
                String id = rs.getString("id");
                EndUserCache cache = this.getUser(id);
                users.add(cache);
            }
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }

        return users;
    }

    public void updateUser(String name, EndUserCache cache)
    throws DemeterException, Throwable
    {
        if(false == this.isRegisteredUser(name))
        {
            throw new DemeterException("END USER CANNOT BE FOUND: " + name);
        }
        
        this.caches.put(name, cache);
    }
    
    public synchronized Lock getLock(String name)
    throws DemeterException, Throwable
    {
        if(false == this.isRegisteredUser(name))
        {
            throw new DemeterException("END USER CANNOT BE FOUND: " + name);
        }

        Lock locker = null;
        do
        {
            // ensure that the cache exists
            EndUserCache cache = this.getUser(name);
            if(null == cache)
            {
                break;
            }
            
            locker = CacheFactory.getLock(name, this.caches);
        }
        while(false);
        
        return locker;
    }

    public EndUserCache createUser(
            String name, 
            String password, 
            EndUserRole endUserRole)
    throws DemeterException, Throwable
    {
        EndUserCache cache = null;
        if(true == this.isRegisteredUser(name))
        {
            throw new DemeterException("END USER ALREADY EXISTS");
        }

        if(null == endUserRole)
        {
            throw new DemeterException("END USER'S ROLE MUST BE SPECIFIED");
        }
        
        KeyGenerator keyGen = KeyGenerator.getInstance(SECRET_KEY_SPEC);
        keyGen.init(SECRET_KEY_LENGTH, new SecureRandom());

        // new secret key based on AES-256
        SecretKey secretKey = keyGen.generateKey();
        byte[] encryptedPasswordData = Algorithm.AES256.encrypt(
            secretKey, password.getBytes(StandardCharsets.UTF_8));

        final String secretKeyBase64String = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        final String encryptedPassword = Base64.getEncoder().encodeToString(encryptedPasswordData);
        final Long creationTime = System.currentTimeMillis();

        this.insertEndUser(
            name, 
            UUID.randomUUID().toString(),
            endUserRole.toString(), 
            secretKeyBase64String, 
            encryptedPassword, 
            1, 
            creationTime);
        
        Lock locker = EndUserManager.getInstance().getLock(name);
        try
        {
            locker.lock();

            cache = this.getUser(name);
            cache.setUpdatedTime(creationTime);
            cache.flush();

            this.caches.put(name, cache);
        }
        finally
        {
            locker.unlock();
        }
        
        return cache;
    }

    public void setUser(
            String name, 
            String password, 
            EndUserRole role, 
            int valid)
    throws DemeterException, Throwable
    {
        this.setPassword(name, password);

        EndUserCache endUserCache = this.getUser(name);

        Lock locker = EndUserManager.getInstance().getLock(name);
        try
        {
            locker.lock();

            endUserCache = this.getUser(name);
            endUserCache.setEndUserRole(role);
            endUserCache.setValid(valid);

            endUserCache.flush();

            this.caches.put(name, endUserCache);
        }
        finally
        {
            locker.unlock();
        }
    }
   
    public void deleteUser(String name)
    throws DemeterException, Throwable
    {
        if(false == this.isRegisteredUser(name))
        {
            throw new DemeterException("END USER CANNOT BE FOUND: " + name);
        }
        
        Lock locker = EndUserManager.getInstance().getLock(name);
        try
        {
            locker.lock();

            // delete from database
            deleteEndUser(name);
        }
        finally
        {
            locker.unlock();
        }

        // remove from memory cache
        this.caches.remove(name);
    }

    public String getPassword(String name)
    throws DemeterException, Throwable
    {
        String password;
        if(false == this.isRegisteredUser(name))
        {
            throw new DemeterException("END USER CANNOT BE FOUND: " + name);
        }

        EndUserCache cache = this.getUser(name);
        byte[] secretKeyData = Base64.getDecoder().decode(cache.getStoredKey());
        byte[] cipherData = Base64.getDecoder().decode(cache.getEncryptedPassword());
        
        SecretKey secretKey = new SecretKeySpec(secretKeyData, 0, secretKeyData.length, "AES");
        byte[] plainData = Algorithm.AES256.decrypt(secretKey, cipherData);
        password = new String(plainData);

        return password;
    }
    
    public void setPassword(String name, String password)
    throws DemeterException, Throwable
    {
        if(false == this.isRegisteredUser(name))
        {
            throw new DemeterException("END USER CANNOT BE FOUND: " + name);
        }

        Lock locker = EndUserManager.getInstance().getLock(name);
        try
        {
            locker.lock();
            
            EndUserCache cache = this.getUser(name);
            
            KeyGenerator keyGen = KeyGenerator.getInstance(SECRET_KEY_SPEC);
            keyGen.init(SECRET_KEY_LENGTH, new SecureRandom());

            // new secret key based on AES-256
            SecretKey secretKey = keyGen.generateKey();
            byte[] encryptedPasswordData = Algorithm.AES256.encrypt(
                secretKey, password.getBytes(StandardCharsets.UTF_8));

            String secretKeyBase64String = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            String encryptedPassword = Base64.getEncoder().encodeToString(encryptedPasswordData);
            
            cache.setStoredKey(secretKeyBase64String);
            cache.setEncryptedPassword(encryptedPassword);
            cache.flush();

            this.caches.put(name, cache);
        }
        finally
        {
            locker.unlock();
        }        
    }
    
    private void insertEndUser(
            String name,
            String uuid,
            String role,
            String storedKey,
            String encryptedPassword,
            Integer valid,
            Long creationTime)
    throws DemeterException, Throwable
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_INSERT_END_USER);
            
            int idx = 0;
            stmt.setString(++idx, name);
            stmt.setString(++idx, uuid);
            stmt.setString(++idx, role);
            stmt.setString(++idx, storedKey);
            stmt.setString(++idx, encryptedPassword);
            stmt.setInt(++idx, valid);
            stmt.setLong(++idx, creationTime);
            
            stmt.executeUpdate();
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }
    }

    private void deleteEndUser(
            String name)
    throws DemeterException, Throwable
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_DELETE_END_USER);
            
            int idx = 0;
            stmt.setString(++idx, name);
            
            stmt.executeUpdate();
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }
    }

    private String queryEndUserName(
            String uuid)
    throws DemeterException, Throwable
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_END_USER_NAME);
            
            int idx = 0;
            stmt.setString(++idx, uuid);
            
            rs = stmt.executeQuery();
            if(rs.next())
            {
                return rs.getString("id");
            }
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }
        
        return null;
    }
}
