package com.sercomm.openfire.plugin.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import org.jivesoftware.database.DbConnectionManager;

import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.Log;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.SystemProperties;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.data.frontend.AppInstallation;
import com.sercomm.openfire.plugin.data.frontend.AppVersion;
import com.sercomm.openfire.plugin.define.DeviceState;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.profile.IProfile;
import com.sercomm.openfire.plugin.profile.Profile;

public class InstallAppTask extends TimerTask
{
    public interface Listener
    {
        void onDelivered(
                String serial,
                String mac,
                String appId,
                String versionId,
                long triggerTime);
        void onInstalling(
                String serial,
                String mac,
                String appId,
                String versionId,
                long triggerTime);
        void onCompleted(
                String serial,
                String mac,
                String appId,
                String versionId,
                long triggerTime);
        void onTimeout(
                String serial,
                String mac,
                String appId,
                String versionId,
                long triggerTime);
        void onFail(
                String serial,
                String mac,
                String appId,
                String versionId,
                String errorMessage,
                long triggerTime);
    }

    private String serial;
    private String mac;
    private App app;
    private AppVersion version;
    private long startTime = 0L;
    private long maxInterval = 0L;
    private Listener listener = null;
        
    private final static String TABLE_S_APP_VERSION = "sAppVersion";
    private final static String SQL_INCREASE_APP_INSTALLED_COUNT =
            String.format("UPDATE `%s` SET `installedCount`=`installedCount`+1 WHERE `id`=? AND `appId`=?",
                TABLE_S_APP_VERSION);

    public InstallAppTask(
            String serial,
            String mac,
            App app,
            AppVersion version,
            long maxInterval,
            Listener listener)
    {
        this.serial = serial;
        this.mac = mac;
        this.app = app;
        this.version = version;
        this.maxInterval = maxInterval;
        this.listener = listener;
    }
    
    @Override
    public void run()
    {
        this.startTime = System.currentTimeMillis();
        
        String errorMessage = XStringUtil.BLANK;;
        try
        {
            DeviceCache deviceCache = DeviceManager.getInstance().getDeviceCache(serial, mac);
            if(null == deviceCache)
            {
                throw new DemeterException("DEVICE CANNOT BE FOUND");
            }
            
            if(0 != DeviceState.ONLINE.compareTo(deviceCache.getDeviceState()))
            {
                throw new DemeterException("DEVICE IS NOT AVAILABLE TEMPORARILY");
            }
            
            // query if the App was installed
            AppInstallation appInstallation = null;
            try
            {
                appInstallation = DeviceManager.getInstance().getInstalledApp(
                    this.serial, 
                    this.mac, 
                    this.app.getName());
            }
            catch(Throwable ignored) {}
            
            if(null != appInstallation)
            {
                throw new DemeterException("APP HAS ALREADY BEEN INSTALLED");
            }
            
            final String modelName = deviceCache.getModelName();
            IProfile profile = Profile.get(modelName);

            // ---> trigger ubus: begin
            // make payload string
            com.sercomm.openfire.plugin.data.ubus.Credentials credentials = 
                    new com.sercomm.openfire.plugin.data.ubus.Credentials();
            credentials.Username = XStringUtil.BLANK;
            credentials.Password = XStringUtil.BLANK;
            
            com.sercomm.openfire.plugin.data.ubus.Source source =
                    new com.sercomm.openfire.plugin.data.ubus.Source();
            // must be HTTP
            source.Protocol = SystemProperties.getInstance().getStorageScheme();
            source.Address = SystemProperties.getInstance().getHostServiceAPI().getAddress();
            source.Port = SystemProperties.getInstance().getHostServiceAPI().getPort().toString();
            // must be end with its filename
            source.Resource = String.format("/api/v1/files/package/%s/%s", version.getId(), version.getFilename());
            source.Credentials = credentials;
            
            Map<String, Object> payload = new HashMap<String, Object>();
            payload.put("Id", app.getName());
            payload.put("UUID", version.getId());
            payload.put("Source", source);
            payload.put("DisableAutoStart", false);

            final int UBUS_MAX_RETRY_COUNT = 2;
            
            // send "install" command to device
            for(int counter = 1; counter <= UBUS_MAX_RETRY_COUNT; counter++)
            {
                try
                {
                    Map<?,?> dataModel = profile.installApp(
                        this.serial, 
                        this.mac, 
                        this.app.getName(), 
                        payload);
                    
                    com.sercomm.openfire.plugin.data.ubus.Response response = 
                            Json.mapper().convertValue(dataModel, com.sercomm.openfire.plugin.data.ubus.Response.class);
                    
                    if("OK".compareTo(response.Header.Name) == 0)
                    {
                        // command delivered successfully
                        break;
                    }
                    
                    // throw an error
                    throw new DemeterException("COMMAND IS NOT EXECUTED SUCCESSFULLY: " + response.Header.Name);
                }
                catch(Throwable t1)
                {
                    if(UBUS_MAX_RETRY_COUNT == counter)
                    {
                        throw new DemeterException("REMOTE GATEWAY WAS NOT AVAILABLE TEMPORARILY: " + t1.getMessage());
                    }
                    else
                    {
                        continue;
                    }
                }
            }

            // notify "delivered"
            try
            {
                this.listener.onDelivered(
                    this.serial, 
                    this.mac, 
                    this.app.getId(),
                    this.version.getId(), 
                    System.currentTimeMillis());
            }
            catch(Throwable ignored) {}

            Connection conn = null;
            PreparedStatement stmt = null;
            try
            {
                conn = DbConnectionManager.getConnection();

                int idx = 0;
                stmt = conn.prepareStatement(SQL_INCREASE_APP_INSTALLED_COUNT);                
                stmt.setString(++idx, this.version.getId());
                stmt.setString(++idx, this.app.getId());                
                stmt.executeUpdate();
            }
            finally
            {
                DbConnectionManager.closeConnection(stmt, conn);
            }
            
            // notify "installing"
            try
            {
                this.listener.onInstalling(
                    this.serial, 
                    this.mac, 
                    this.app.getId(),
                    this.version.getId(), 
                    System.currentTimeMillis());
            }
            catch(Throwable ignored) {}

            // 5 seconds for gateway to initialize the installation process
            Thread.sleep(5000L);

            // check if the gateway has installed the IPK properly
            boolean isInstalled = false;
            do
            {
                if(System.currentTimeMillis() - this.startTime > this.maxInterval)
                {
                    // timeout
                    break;
                }
                
                appInstallation = null;
                try
                {
                    appInstallation = DeviceManager.getInstance().getInstalledApp(
                        this.serial, 
                        this.mac, 
                        this.app.getName());
                }
                catch(Throwable ignored) {}

                if(null == appInstallation)
                {
                    // maybe it is ongoing
                    // retry
                    continue;
                }
                
                if(0 == "Installed".compareToIgnoreCase(appInstallation.getStatus()) ||
                   0 == "Running".compareToIgnoreCase(appInstallation.getStatus()))
                {
                    isInstalled = true;
                    break;
                }                
            }
            while(true);

            if(false == isInstalled)
            {
                errorMessage = "TIMEOUT";

                // notify "timeout"
                try
                {
                    this.listener.onTimeout(
                        this.serial, 
                        this.mac, 
                        this.app.getId(),
                        this.version.getId(), 
                        System.currentTimeMillis());
                }
                catch(Throwable ignored) {}
            }
            else
            {
                // notify "completed"
                try
                {
                    this.listener.onCompleted(
                        this.serial, 
                        this.mac, 
                        this.app.getId(),
                        this.version.getId(), 
                        System.currentTimeMillis());
                }
                catch(Throwable ignored) {}
            }
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();

            // notify "fail"
            try
            {
                this.listener.onFail(
                    this.serial, 
                    this.mac, 
                    this.app.getId(),
                    this.version.getId(), 
                    errorMessage, 
                    System.currentTimeMillis());
            }
            catch(Throwable ignored) {}
        }
        catch(Throwable t)
        {
            errorMessage = t.getMessage();

            // notify "fail"
            try
            {
                this.listener.onFail(
                    this.serial, 
                    this.mac, 
                    this.app.getId(),
                    this.version.getId(), 
                    errorMessage, 
                    System.currentTimeMillis());                    
            }
            catch(Throwable ignored) {}
        }
        
        Log.write().info("({},{},{},{},{})={}",
            this.serial,
            this.mac,
            this.app.getPublisher(),
            this.app.getName(),
            this.version.getVersion(),
            errorMessage);
    }
    
    public String getSerial()
    {
        return this.serial;
    }

    public String getMac()
    {
        return this.mac;
    }

    public App getApp()
    {
        return this.app;
    }

    public AppVersion getAppVersion()
    {
        return this.version;
    }

    public long getStartTime()
    {
        return this.startTime;
    }

    public long getMaxInterval()
    {
        return this.maxInterval;
    }
}
