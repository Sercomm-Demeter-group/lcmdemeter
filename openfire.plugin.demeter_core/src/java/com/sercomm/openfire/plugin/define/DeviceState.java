package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum DeviceState
{
    OFFLINE("offline"),
    ONLINE("online");

    private static Map<String, DeviceState> __map = 
            new ConcurrentHashMap<String, DeviceState>();
    static
    {
        for(DeviceState deviceState : DeviceState.values())
        {
            __map.put(deviceState.name(), deviceState);
        }
    }

    private String value;
    private DeviceState(String value)
    {
        this.value = value;
    }
    
    public static DeviceState fromString(String value)
    {
        return __map.get(value);
    }
    
    @Override
    public String toString()
    {
        return this.value;
    }
}
