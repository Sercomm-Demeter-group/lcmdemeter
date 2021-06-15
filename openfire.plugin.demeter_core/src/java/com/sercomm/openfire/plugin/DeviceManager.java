package com.sercomm.openfire.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.event.UserEventDispatcher;
import org.jivesoftware.openfire.event.UserEventListener;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.util.DateTime;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.data.frontend.AppInstallation;
import com.sercomm.openfire.plugin.data.frontend.AppVersion;
import com.sercomm.openfire.plugin.define.AppAction;
import com.sercomm.openfire.plugin.define.DeviceState;
import com.sercomm.openfire.plugin.dispatcher.DeviceEnrollDispatcher;
import com.sercomm.openfire.plugin.dispatcher.DeviceStateDispatcher;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.profile.IProfile;
import com.sercomm.openfire.plugin.profile.Profile;
import com.sercomm.openfire.plugin.task.InstallAppTask;
import com.sercomm.openfire.plugin.task.UpdateAppTask;
import com.sercomm.openfire.plugin.util.DbConnectionUtil;

public class DeviceManager extends ManagerBase 
{
    private static final Logger log = LoggerFactory.getLogger(DeviceManager.class);

    private final static String CACHE_NAME = "Demeter Device Caches";
    private Cache<String, DeviceCache> deviceCaches;

    private final static String TABLE_S_APP_VERSION = "sAppVersion";
    private final static String TABLE_S_DEVICE_PROP = "sDeviceProp";

    private final static String SQL_INCREASE_APP_REMOVED_COUNT =
            String.format("UPDATE `%s` SET `removedCount`=`removedCount`+1 WHERE `version`=? AND `appId`=?",
                TABLE_S_APP_VERSION);
    private final static String SQL_QUERY_DISTINCT_MODEL_NAMES =
            String.format("SELECT DISTINCT(`propValue`) FROM `%s` WHERE `name`='sercomm.device.model.name';",
                TABLE_S_DEVICE_PROP);

    private final static int MAX_RETRY_COUNT = 2;
    
    private final UserEventListener userEventListener = new UserEventListener()
    {
        @Override
        public void userCreated(
                User user,
                Map<String, Object> params)
        {
        }

        @Override
        public void userDeleting(
                User user,
                Map<String, Object> params)
        {
            final String nodeName = user.getUsername();
            final String serial = NameRule.toDeviceSerial(nodeName);
            final String mac = NameRule.toDeviceMac(nodeName);
            try
            {
                DeviceCache cache = DeviceManager.getInstance().getDeviceCache(serial, mac);
                
                DeviceEnrollDispatcher.dispatchDeleted(serial, mac, cache.getModelName());

                cache.deleteProperties();
            }
            catch(Throwable t)
            {
                log.error(t.getMessage(), t);
            }            
        }

        @Override
        public void userModified(
                User user,
                Map<String, Object> params)
        {
        }
    };
    
    private final SessionEventListener sessionEventListener = new SessionEventListener()
    {
        @Override
        public void sessionCreated(Session session)
        {
            final JID deviceJID = session.getAddress();
            final String nodeName = deviceJID.getNode();
            final String serial = NameRule.toDeviceSerial(nodeName);
            final String mac = NameRule.toDeviceMac(nodeName);
            
            DeviceState newState = DeviceState.ONLINE;            
            String errorMessage = XStringUtil.BLANK;            
            try
            {
                Lock locker = DeviceManager.getInstance().getLock(serial, mac);
                try
                {
                    locker.lock();
                    do
                    {
                        DeviceCache deviceCache = 
                                DeviceManager.getInstance().getDeviceCache(serial, mac);
                        DeviceState oldState = deviceCache.getDeviceState();
                        if(0 == oldState.compareTo(newState))
                        {
                            break;
                        }
                        
                        long currentTime = System.currentTimeMillis();

                        deviceCache.setDeviceState(newState);
                        deviceCache.setLastOnlineTime(currentTime);
                        deviceCache.flush();
                        
                        DeviceStateDispatcher.dispatchStateChanged(serial, mac, oldState, newState);
                    }
                    while(false);
                }
                finally
                {
                    locker.unlock();
                }                
            }
            catch(Throwable t)
            {
                log.error(t.getMessage(), t);
                errorMessage = t.getMessage();
            }
            
            log.debug("({},{})={}", serial, mac, errorMessage);
        }

        @Override
        public void sessionDestroyed(Session session)
        {
            final JID deviceJID = session.getAddress();
            final String nodeName = deviceJID.getNode();
            final String serial = NameRule.toDeviceSerial(nodeName);
            final String mac = NameRule.toDeviceMac(nodeName);
            
            DeviceState newState = DeviceState.OFFLINE;            
            String errorMessage = XStringUtil.BLANK;            
            try
            {
                Lock locker = DeviceManager.getInstance().getLock(serial, mac);
                try
                {
                    locker.lock();
                    do
                    {
                        DeviceCache deviceCache = 
                                DeviceManager.getInstance().getDeviceCache(serial, mac);
                        DeviceState oldState = deviceCache.getDeviceState();
                        if(0 == oldState.compareTo(newState))
                        {
                            break;
                        }
                        
                        long currentTime = System.currentTimeMillis();

                        deviceCache.setDeviceState(newState);
                        deviceCache.setLastOfflineTime(currentTime);
                        deviceCache.flush();
                        
                        DeviceStateDispatcher.dispatchStateChanged(serial, mac, oldState, newState);
                    }
                    while(false);
                }
                finally
                {
                    locker.unlock();
                }                
            }
            catch(Throwable t)
            {
                log.error(t.getMessage(), t);
                errorMessage = t.getMessage();
            }

            log.debug("({},{})={}", serial, mac, errorMessage);
        }

        @Override
        public void anonymousSessionCreated(Session session)
        {
        }

        @Override
        public void anonymousSessionDestroyed(Session session)
        {
        }

        @Override
        public void resourceBound(Session session)
        {
        }
    };
    
    private static class DeviceManagerContainer
    {
        private final static DeviceManager instance = new DeviceManager();
    }

    private DeviceManager()
    {
    }
    
    public static DeviceManager getInstance()
    {
        return DeviceManagerContainer.instance;
    }

    @SuppressWarnings("all")
    @Override
    protected void onInitialize()
    {
        this.deviceCaches = CacheFactory.createCache(CACHE_NAME);
        this.deviceCaches.setMaxCacheSize(-1);
        this.deviceCaches.setMaxLifetime(86400 * 1000L);
        
        UserEventDispatcher.addListener(this.userEventListener);
        SessionEventDispatcher.addListener(this.sessionEventListener);        
    }

    @Override
    protected void onUninitialize()
    {
        SessionEventDispatcher.removeListener(this.sessionEventListener);
        UserEventDispatcher.removeListener(this.userEventListener);
    }

    public DeviceCache getDeviceCache(String serial, String mac)
    throws DemeterException, UserNotFoundException
    {
        final String nodeName = NameRule.formatDeviceName(serial, mac);
        
        return this.getDeviceCache(nodeName);
    }
    
    public DeviceCache getDeviceCache(String nodeName)
    throws DemeterException, UserNotFoundException
    {
        if(false == UserManager.getInstance().isRegisteredUser(nodeName))
        {
            throw new DemeterException("DEVICE CANNOT BE FOUND");
        }
        
        DeviceCache cache = this.deviceCaches.get(nodeName);
        if(null == cache)
        {
            try
            {
                cache = new DeviceCache(
                    NameRule.toDeviceSerial(nodeName), 
                    NameRule.toDeviceMac(nodeName));

                this.deviceCaches.put(nodeName, cache);
            }
            catch(SQLException e)
            {
                throw new DemeterException(e.getMessage());
            }
        }
        
        return cache;
    }    
    
    public void updateDeviceCache(String serial, String mac, DeviceCache deviceCache)
    throws DemeterException
    {
        final String nodeName = NameRule.formatDeviceName(serial, mac);
        
        this.updateDeviceCache(nodeName, deviceCache);
    }

    public void updateDeviceCache(String nodeName, DeviceCache deviceCache)
    throws DemeterException
    {
        if(false == UserManager.getInstance().isRegisteredUser(nodeName))
        {
            throw new DemeterException("DEVICE CANNOT BE FOUND");
        }
        
        if(false == this.deviceCaches.containsKey(nodeName))
        {
            throw new DemeterException("CACHE CANNOT BE FOUND: " + nodeName);
        }
        
        this.deviceCaches.put(nodeName, deviceCache);
    }

    public synchronized Lock getLock(String serial, String mac)
    throws DemeterException, UserNotFoundException
    {
        final String nodeName = NameRule.formatDeviceName(serial, mac);
        if(false == UserManager.getInstance().isRegisteredUser(nodeName))
        {
            throw new DemeterException("DEVICE CANNOT BE FOUND");
        }
        
        Lock locker = null;
        do
        {
            // ensure that the cache exists
            DeviceCache cache = this.getDeviceCache(serial, mac);
            if(null == cache)
            {
                break;
            }
            
            locker = CacheFactory.getLock(nodeName, this.deviceCaches);
        }
        while(false);
        
        return locker;
    }
    
    // install latest version of the App
    // blocking call
    public void installApp(String serial, String mac, String appId)
    throws DemeterException, Throwable
    {        
        final App app = AppManager.getInstance().getApp(appId);                
        final AppVersion version = AppManager.getInstance().getAppLatestVersion(appId);

        final BlockingInstallListener listener = new BlockingInstallListener();
        InstallAppTask installTask = new InstallAppTask(
            serial,
            mac,
            app,
            version,
            60 * 1000L,
            listener);
       
        TaskEngine.getInstance().schedule(installTask, 0L);
        
        synchronized(listener)
        {
            listener.wait();
        }
        
        if(XStringUtil.isNotBlank(listener.errorMessage))
        {
            throw new DemeterException(listener.errorMessage);
        }
    }

    // install specific version of the App
    // blocking call
    public void installApp(String serial, String mac, String appId, String versionId)
    throws DemeterException, Throwable
    {        
        final App app = AppManager.getInstance().getApp(appId);                
        final AppVersion version = AppManager.getInstance().getAppVersion(versionId);

        final BlockingInstallListener listener = new BlockingInstallListener();
        InstallAppTask installTask = new InstallAppTask(
            serial,
            mac,
            app,
            version,
            60 * 1000L,
            listener);
       
        TaskEngine.getInstance().schedule(installTask, 0L);
        
        synchronized(listener)
        {
            listener.wait();
        }
        
        if(XStringUtil.isNotBlank(listener.errorMessage))
        {
            throw new DemeterException(listener.errorMessage);
        }
    }

    // install the specific version of the App
    // asynchronous call with listener
    public void installApp(
            String serial, 
            String mac, 
            String appId, 
            String versionId, 
            InstallAppTask.Listener listener)
    throws DemeterException, Throwable
    {
        final App app = AppManager.getInstance().getApp(appId);
        if(null == app)
        {
            throw new DemeterException("INVALID APP ID");
        }
                
        final AppVersion version = AppManager.getInstance().getAppVersion(versionId);
        if(null == version)
        {
            throw new DemeterException("INVALID VERSION ID");
        }

        InstallAppTask installTask = new InstallAppTask(
            serial, 
            mac, 
            app, 
            version, 
            60 * 1000L, 
            listener);
        
        TaskEngine.getInstance().schedule(installTask, 0L);
    }

    public void updateApp(
            String serial, 
            String mac, 
            String appId, 
            String versionId, 
            UpdateAppTask.Listener listener)
    throws DemeterException, Throwable
    {
        final App app = AppManager.getInstance().getApp(appId);
        if(null == app)
        {
            throw new DemeterException("INVALID APP ID");
        }
                
        final AppVersion version = AppManager.getInstance().getAppVersion(versionId);
        if(null == version)
        {
            throw new DemeterException("INVALID VERSION ID");
        }

        UpdateAppTask patchTask = new UpdateAppTask(
            serial, 
            mac, 
            app, 
            version, 
            60 * 1000L, 
            listener);
        
        TaskEngine.getInstance().schedule(patchTask, 0L);
    }
    
    public void uninstallApp(String serial, String mac, String appId)
    throws DemeterException, Throwable
    {
        DeviceCache deviceCache = this.getDeviceCache(serial, mac);
        if(null == deviceCache)
        {
            throw new DemeterException("DEVICE CANNOT BE FOUND");
        }
        
        if(0 != DeviceState.ONLINE.compareTo(deviceCache.getDeviceState()))
        {
            throw new DemeterException("DEVICE IS UNAVAILABLE TEMPORARILY");
        }        
        
        final App app = AppManager.getInstance().getApp(appId);
        if(null == app)
        {
            throw new DemeterException("INVALID APP ID");
        }

        final AppInstallation installation = this.getInstalledApp(serial, mac, app.getName());
        if(null == installation)
        {
            throw new DemeterException("APP HAS NOT BEEN INSTALLED");
        }
        
        final String modelName = deviceCache.getModelName();
        IProfile profile = Profile.get(modelName);

        Lock locker = DeviceManager.getInstance().getLock(serial, mac);
        try
        {
            locker.lock();
            
            // update database
            boolean abortTransaction = true;
            Connection conn = null;
            PreparedStatement stmt = null;
            try
            {
                conn = DbConnectionManager.getConnection();
                conn = DbConnectionUtil.openTransaction(conn);
                                
                int idx = 0;
                stmt = conn.prepareStatement(SQL_INCREASE_APP_REMOVED_COUNT);                
                stmt.setString(++idx, installation.getVersion());
                stmt.setString(++idx, appId);                
                stmt.executeUpdate();
                
                // send command to gateway
                for(int counter = 1; counter <= MAX_RETRY_COUNT; counter++)
                {
                    try
                    {
                        Map<?,?> dataModel = profile.uninstallApp(serial, mac, app.getName());
                        com.sercomm.openfire.plugin.data.ubus.Response response = 
                                Json.mapper().convertValue(dataModel, com.sercomm.openfire.plugin.data.ubus.Response.class);
                        if("OK".compareTo(response.Header.Name) == 0)
                        {
                            // install successfully
                            break;
                        }
                        
                        // throw an error
                        throw new DemeterException("COMMAND IS NOT EXECUTED SUCCESSFULLY: " + response.Header.Name);
                    }
                    catch(Throwable t1)
                    {
                        if(MAX_RETRY_COUNT == counter)
                        {
                            throw new DemeterException("REMOTE GATEWAY WAS NOT AVAILABLE TEMPORARILY: " + t1.getMessage());
                        }
                        else
                        {
                            continue;
                        }
                    }
                }

                abortTransaction = false;
                
                // wait the gateway to proceed the procedure
                Thread.sleep(1 * 1000L);
            }
            finally
            {
                DbConnectionManager.closeStatement(stmt);
                DbConnectionUtil.closeTransaction(conn, abortTransaction);
                DbConnectionManager.closeConnection(conn);
            }
        }
        finally
        {
            locker.unlock();
        }
    }
    
    public List<String> getInstalledVersionIds(String serial, String mac)
    throws DemeterException, Throwable
    {
        // collection<app version id>
        List<String> collection = new ArrayList<String>();
        
        DeviceCache deviceCache = this.getDeviceCache(serial, mac);
        if(null == deviceCache)
        {
            throw new DemeterException("DEVICE CANNOT BE FOUND");
        }
        
        if(0 != DeviceState.ONLINE.compareTo(deviceCache.getDeviceState()))
        {
            throw new DemeterException("DEVICE IS UNAVAILABLE TEMPORARILY");
        }
        
        final String modelName = deviceCache.getModelName();
        IProfile profile = Profile.get(modelName);

        // send command to gateway
        List<Map<?,?>> dataModels = null;
        for(int counter = 1; counter <= MAX_RETRY_COUNT; counter++)
        {
            try
            {
                dataModels = profile.getInstalledApps(serial, mac);
                break;
            }
            catch(Throwable t1)
            {
                if(MAX_RETRY_COUNT == counter)
                {
                    throw new DemeterException("REMOTE GATEWAY WAS NOT AVAILABLE TEMPORARILY: " + t1.getMessage());
                }
                else
                {
                    continue;
                }
            }
        }
        
        for(Map<?,?> dataModel : dataModels)
        {
            com.sercomm.openfire.plugin.data.ubus.Response response = 
                    Json.mapper().convertValue(dataModel, com.sercomm.openfire.plugin.data.ubus.Response.class);
            
            if("OK".compareTo(response.Header.Name) == 0)
            {
                com.sercomm.openfire.plugin.data.ubus.Packages packages =
                        Json.mapper().convertValue(response.Body, com.sercomm.openfire.plugin.data.ubus.Packages.class);
                
                for(com.sercomm.openfire.plugin.data.ubus.Packages.Package _package : packages.List)
                {
                    // obtain AppID
                    AppVersion version = AppManager.getInstance().getAppVersion(_package.UUID);
                    if(null == version)
                    {
                        // not supported version
                        continue;
                    }
                    String appId = version.getAppId();
                    
                    // current installed version
                    version = AppManager.getInstance().getAppVersion(appId, _package.Version);
                    if(null == version)
                    {
                        // not supported version
                        continue;
                    }
                    
                    collection.add(version.getId());
                }
            }
        }
        
        return collection;
    }
        
    public AppInstallation getInstalledApp(
            String serial,
            String mac,
            String appName)
    throws DemeterException, Throwable
    {
        DeviceCache deviceCache = this.getDeviceCache(serial, mac);
        if(null == deviceCache)
        {
            throw new DemeterException("DEVICE CANNOT BE FOUND");
        }
        
        if(0 != DeviceState.ONLINE.compareTo(deviceCache.getDeviceState()))
        {
            throw new DemeterException("DEVICE IS UNAVAILABLE TEMPORARILY");
        }
        
        final String modelName = deviceCache.getModelName();
        IProfile profile = Profile.get(modelName);

        // send command to gateway
        Map<?, ?> dataModel = null;
        for(int counter = 1; counter <= MAX_RETRY_COUNT; counter++)
        {
            try
            {
                dataModel = profile.getInstalledApp(serial, mac, appName);
                break;
            }
            catch(Throwable t1)
            {
                if(MAX_RETRY_COUNT == counter)
                {
                    throw new DemeterException("REMOTE GATEWAY WAS NOT AVAILABLE TEMPORARILY: " + t1.getMessage());
                }
                else
                {
                    continue;
                }
            }
        }

        com.sercomm.openfire.plugin.data.ubus.Response response = 
                Json.mapper().convertValue(dataModel, com.sercomm.openfire.plugin.data.ubus.Response.class);
        
        AppInstallation appInstallation = null;        

        if("OK".compareTo(response.Header.Name) == 0)
        {
            com.sercomm.openfire.plugin.data.ubus.Packages.Package _package =
                    Json.mapper().convertValue(response.Body, com.sercomm.openfire.plugin.data.ubus.Packages.Package.class);

            String timestamp = XStringUtil.replaceLast(_package.Install.Timestamp, ":", XStringUtil.BLANK);
            DateTime updatedTime = DateTime.from(timestamp, DateTime.FORMAT_ISO);
            
            // WARNING:
            // UUID is the 1st installed version ID, but maybe the installed version has ever been updated
            // so it is safe for obtaining the App ID, but it is NOT SAFE for obtaining the version entity
            AppVersion appVersion = AppManager.getInstance().getAppVersion(_package.UUID);
            
            appInstallation = new AppInstallation();
            appInstallation.setAppId(appVersion.getAppId());
            appInstallation.setSerial(serial);
            appInstallation.setMac(mac);
            appInstallation.setVersion(_package.Version);
            appInstallation.setStatus(XStringUtil.isBlank(_package.Status) ? XStringUtil.BLANK : _package.Status);
            appInstallation.setExecuted(0 == "Running".compareTo(_package.Status) ? 1 : 0);
            appInstallation.setUpdatedTime(updatedTime.getTimeInMillis());
        }
        
        return appInstallation;
    }
    
    public List<String> getAvailableModelNames()
    throws DemeterException, Throwable
    {
        List<String> modelNames = new ArrayList<String>();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_DISTINCT_MODEL_NAMES);
            
            rs = stmt.executeQuery();
            while(rs.next())
            {
                modelNames.add(rs.getString("propValue"));
            }
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }

        return modelNames;
    }
    
    public void controlApp(
        String serial,
        String mac,
        String appId,
        AppAction appAction)
    throws DemeterException, Throwable
    {
        DeviceCache deviceCache = this.getDeviceCache(serial, mac);
        if(null == deviceCache)
        {
            throw new DemeterException("DEVICE CANNOT BE FOUND");
        }
        
        if(0 != DeviceState.ONLINE.compareTo(deviceCache.getDeviceState()))
        {
            throw new DemeterException("DEVICE IS UNAVAILABLE TEMPORARILY");
        }

        final App app = AppManager.getInstance().getApp(appId);
        if(null == app)
        {
            throw new DemeterException("DEVICE CANNOT BE FOUND");
        }
        
        Lock locker = DeviceManager.getInstance().getLock(serial, mac);
        try
        {
            locker.lock();

            AppInstallation appInstallation = null;
            try
            {
                appInstallation = DeviceManager.getInstance().getInstalledApp(serial, mac, app.getName());
            }
            catch(Throwable ignored) {}

            if(null == appInstallation)
            {
                throw new DemeterException("APP HAS NOT BEEN INSTALLED");
            }
    
            final String modelName = deviceCache.getModelName();
            IProfile profile = Profile.get(modelName);

            switch(appAction)
            {
                case START:
                    {
                        if(1 == appInstallation.getExecuted())
                        {
                            throw new DemeterException("ALREADY STARTED");
                        }

                        // send command to gateway
                        for(int counter = 1; counter <= MAX_RETRY_COUNT; counter++)
                        {
                            try
                            {
                                profile.startApp(serial, mac, app.getName());
                                break;
                            }
                            catch(Throwable t1)
                            {
                                if(MAX_RETRY_COUNT <= counter)
                                {
                                    throw new DemeterException("REMOTE GATEWAY WAS NOT AVAILABLE TEMPORARILY: " + t1.getMessage());
                                }
                                else
                                {
                                    continue;
                                }
                            }
                        }
                    }
                    break;
                case STOP:
                    {
                        if(0 == appInstallation.getExecuted())
                        {
                            throw new DemeterException("ALREADY STOPPED");
                        }

                        // send command to gateway
                        for(int counter = 1; counter <= MAX_RETRY_COUNT; counter++)
                        {
                            try
                            {
                                profile.stopApp(serial, mac, app.getName());
                                break;
                            }
                            catch(Throwable t1)
                            {
                                if(MAX_RETRY_COUNT <= counter)
                                {
                                    throw new DemeterException("REMOTE GATEWAY WAS NOT AVAILABLE TEMPORARILY: " + t1.getMessage());
                                }
                                else
                                {
                                    continue;
                                }
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        finally
        {
            locker.unlock();
        }
    }
    
    private static class BlockingInstallListener implements InstallAppTask.Listener
    {
        private String errorMessage = XStringUtil.BLANK;
        
        @Override
        public void onDelivered(String serial, String mac, String appId, String versionId, long triggerTime)
        {
        }

        @Override
        public void onInstalling(String serial, String mac, String appId, String versionId, long triggerTime)
        {
        }

        @Override
        public void onCompleted(String serial, String mac, String appId, String versionId, long triggerTime)
        {
            synchronized(this)
            {
                this.notifyAll();
            }
        }

        @Override
        public void onTimeout(String serial, String mac, String appId, String versionId, long triggerTime)
        {
            errorMessage = "TIMEOUT";
            
            synchronized(this)
            {
                this.notifyAll();
            }
        }

        @Override
        public void onFail(String serial, String mac, String appId, String versionId, String errorMessage, long triggerTime)
        {
            this.errorMessage = errorMessage;
            
            synchronized(this)
            {
                this.notifyAll();
            }
        }        
    }
}
