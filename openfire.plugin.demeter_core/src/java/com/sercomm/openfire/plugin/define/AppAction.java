package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum AppAction
{
    START("start"),
    STOP("stop");

    private static Map<String, AppAction> __map = 
            new ConcurrentHashMap<String, AppAction>();
    static
    {
        for(AppAction appAction : AppAction.values())
        {
            __map.put(appAction.toString(), appAction);
        }
    }

    private String value;
    private AppAction(String value)
    {
        this.value = value;
    }
    
    @Override
    public String toString()
    {
        return this.value;
    }
    
    public static AppAction fromString(String value)
    {
        return __map.get(value);
    }
}
