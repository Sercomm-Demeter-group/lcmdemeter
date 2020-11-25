package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum AppEventType
{
    SUBSCRIBE("subscribe", "User subscribed app"),
    UNSUBSCRIBE("unsubscribe", "User unsubscribed app"),
    INSTALL("install", "User install app on device"),
    UNINSTALL("uninstall", "User uninstalled app on device"),
    START("start", "User successfully start app on device"),
    STOP("stop", "User successfully stop app on device");
    
    private static Map<String, AppEventType> __map = 
            new ConcurrentHashMap<String, AppEventType>();
    static
    {
        for(AppEventType appEventType : AppEventType.values())
        {
            __map.put(appEventType.toString(), appEventType);
        }
    }

    private String value;
    private String message;
    
    private AppEventType(String value, String message)
    {
        this.value = value;
        this.message = message;
    }
        
    @Override
    public String toString()
    {
        return this.value;
    }

    public String toMessage()
    {
        return this.message;
    }
    
    public static AppEventType fromString(String value)
    {
        return __map.get(value);
    }
}
