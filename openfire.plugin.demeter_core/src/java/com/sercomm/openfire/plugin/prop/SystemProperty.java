package com.sercomm.openfire.plugin.prop;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum SystemProperty
{
    SERCOMM_DEMETER_HOST_SERVICE_API("sercomm.demeter.host.service.api"),
    SERCOMM_DEMETER_HOST_DEVICE_ENTRY("sercomm.demeter.host.device.entry"),
    SERCOMM_DEMETER_STORAGE("sercomm.demeter.storage"),
    SERCOMM_DEMETER_STORAGE_SCHEME("sercomm.demeter.storage.scheme");
    
    private static Map<String, SystemProperty> map = 
            new ConcurrentHashMap<String, SystemProperty>();
    static
    {
        for(SystemProperty cpeProperty : SystemProperty.values())
        {
            map.put(cpeProperty.toString(), cpeProperty);
        }
    }
    
    private String value;
    private SystemProperty(String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return this.value;
    }
    
    public static SystemProperty fromString(String value)
    {
        return map.get(value);
    }
}
