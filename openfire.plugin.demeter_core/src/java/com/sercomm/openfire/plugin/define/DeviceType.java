package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum DeviceType
{
    D_CPE;
    
    private static Map<String, DeviceType> __map = 
            new ConcurrentHashMap<String, DeviceType>();
    static
    {
        for(DeviceType deviceState : DeviceType.values())
        {
            __map.put(deviceState.name(), deviceState);
        }
    }

    public static DeviceType fromString(String value)
    {
        return __map.get(value);
    }
}
