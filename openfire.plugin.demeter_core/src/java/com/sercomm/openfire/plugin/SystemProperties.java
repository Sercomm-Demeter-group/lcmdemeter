package com.sercomm.openfire.plugin;

import java.util.Map;
import java.util.UUID;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.define.StorageType;
import com.sercomm.openfire.plugin.prop.SystemProperty;

public class SystemProperties implements PropertyEventListener
{
    private static final Logger log = LoggerFactory.getLogger(SystemProperties.class);

    private Host hostServiceAPI;
    private Host hostDeviceEntry;
    private Storage storage;
    private String storageScheme;
    private String apiKey;

    private final static class SystemPropertiesContainer
    {
        private final static SystemProperties instance = new SystemProperties();
    }
    
    private final static Host DEFAULT_HOST_SERVICE_API = new Host();
    private final static Host DEFAULT_HOST_DEVICE_ENTRY = new Host();
    private final static Storage DEFAULT_STORAGE = new Storage();
    private final static String DEFAULT_API_KEY = new String(UUID.randomUUID().toString());
    static 
    {
        // default endpoint address information for DSL devices
        DEFAULT_HOST_SERVICE_API.address = "172.19.1.1";
        DEFAULT_HOST_SERVICE_API.port = 443;
        // default endpoint address information for portal web pages
        DEFAULT_HOST_DEVICE_ENTRY.address = "172.19.1.1";
        DEFAULT_HOST_DEVICE_ENTRY.port = 443;
        
        DEFAULT_STORAGE.storageType = StorageType.LOCAL_FS;
        DEFAULT_STORAGE.rootPath = "/opt/demeter/";
        DEFAULT_STORAGE.credential = XStringUtil.BLANK;
        DEFAULT_STORAGE.awsUrl = "";
        DEFAULT_STORAGE.awsKey = "";
        DEFAULT_STORAGE.awsSecret = "";
        DEFAULT_STORAGE.awsBucket = "";

        try
        {
            SystemPropertiesContainer.instance.loadProperties();

            // force to save once in order to make sure the default properties can be set
            SystemPropertiesContainer.instance.saveProperties();
        }
        catch(Throwable t)
        {
            log.error(t.getMessage(), t);
        }
    }
    
    private SystemProperties()
    {
    }
    
    public static SystemProperties getInstance()
    {
        return SystemPropertiesContainer.instance;
    }
        
    public Host getHostServiceAPI()
    {
        return this.hostServiceAPI;
    }

    public Host getHostDeviceEntry()
    {
        return this.hostDeviceEntry;
    }
    
    public Storage getStorage()
    {
        return this.storage;
    }
    
    public String getStorageScheme()
    {
        return this.storageScheme;
    }
    
    public String getApiKey()
    {
        return this.apiKey;
    }

    private void loadProperties()
    throws Throwable
    {
        String hostServiceAPIString = JiveGlobals.getProperty(
            SystemProperty.SERCOMM_DEMETER_HOST_SERVICE_API.toString(), Json.build(DEFAULT_HOST_SERVICE_API));
        String hostDeviceEntryString = JiveGlobals.getProperty(
            SystemProperty.SERCOMM_DEMETER_HOST_DEVICE_ENTRY.toString(), Json.build(DEFAULT_HOST_DEVICE_ENTRY));
        String storageString = JiveGlobals.getProperty(
            SystemProperty.SERCOMM_DEMETER_STORAGE.toString(), Json.build(DEFAULT_STORAGE));
        String storageScheme = JiveGlobals.getProperty(
            SystemProperty.SERCOMM_DEMETER_STORAGE_SCHEME.toString(), "http");
        String apiKey = JiveGlobals.getProperty(
            SystemProperty.SERCOMM_DEMETER_API_KEY.toString(), DEFAULT_API_KEY);
        
        this.hostServiceAPI = Json.mapper().readValue(hostServiceAPIString, Host.class);
        this.hostDeviceEntry = Json.mapper().readValue(hostDeviceEntryString, Host.class);
        this.storage = Json.mapper().readValue(storageString, Storage.class);
        this.storageScheme = storageScheme;
        this.apiKey = apiKey;
    }

    private void saveProperties()
    throws Throwable
    {
        JiveGlobals.setProperty(
            SystemProperty.SERCOMM_DEMETER_HOST_SERVICE_API.toString(), Json.build(this.hostServiceAPI));
        JiveGlobals.setProperty(
            SystemProperty.SERCOMM_DEMETER_HOST_DEVICE_ENTRY.toString(), Json.build(this.hostDeviceEntry));
        JiveGlobals.setProperty(
            SystemProperty.SERCOMM_DEMETER_STORAGE.toString(), Json.build(this.storage));
        JiveGlobals.setProperty(
            SystemProperty.SERCOMM_DEMETER_STORAGE_SCHEME.toString(), this.storageScheme);
        JiveGlobals.setProperty(
            SystemProperty.SERCOMM_DEMETER_API_KEY.toString(), this.apiKey);
    }
    
    @Override
    public void propertySet(String property, Map<String, Object> params)
    {
        if(property.startsWith("sercomm"))
        {
            try
            {
                SystemPropertiesContainer.instance.loadProperties();       
            }
            catch(Throwable t)
            {
                log.error(t.getMessage(), t);
            }
        }
    }

    @Override
    public void propertyDeleted(String property, Map<String, Object> params)
    {
        if(property.startsWith("sercomm"))
        {
            try
            {
                SystemPropertiesContainer.instance.loadProperties();       
            }
            catch(Throwable t)
            {
                log.error(t.getMessage(), t);
            }
        }
    }

    @Override
    public void xmlPropertySet(String property, Map<String, Object> params)
    {
    }

    @Override
    public void xmlPropertyDeleted(String property, Map<String, Object> params)
    {
    }
    
    public static class Host
    {
        private String address;
        private Integer port;
        
        public String getAddress()
        {
            return this.address;
        }
        
        public Integer getPort()
        {
            return this.port;
        }
    }
    
    public static class Storage
    {
        private StorageType storageType;
        private String rootPath;
        private String credential;
        private String awsUrl;
        private String awsKey;
        private String awsSecret;
        private String awsBucket;

        public StorageType getStorageType()
        {
            return this.storageType;
        }
        
        public String getRootPath()
        {
            return this.rootPath;
        }
        
        public String getCredential()
        {
            return this.credential;
        }
        public String getAwsUrl()
        {
            return this.awsUrl;
        }
        public String getAwsKey()
        {
            return this.awsKey;
        }
        public String getAwsSecret()
        {
            return this.awsSecret;
        }
        public String getAwsBucket()
        {
            return this.awsBucket;
        }
    }
}
