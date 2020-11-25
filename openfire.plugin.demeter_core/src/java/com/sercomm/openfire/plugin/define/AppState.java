package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum AppState
{
    INSTALLED("Installed"),
    RUNNING("Running");
    
    private static Map<String, AppState> __map = 
            new ConcurrentHashMap<String, AppState>();
    static
    {
        for(AppState appState : AppState.values())
        {
            __map.put(appState.toString(), appState);
        }
    }

    private String value;
    private AppState(String value)
    {
        this.value = value;
    }
    
    @Override
    public String toString()
    {
        return this.value;
    }
    
    public static AppState fromString(String value)
    {
        return __map.get(value);
    }
}
