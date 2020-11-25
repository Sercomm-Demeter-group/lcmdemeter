package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum SystemRecordType
{
    SERVER_STARTUP,
    SERVER_SHUTDOWN,

    DEVICE_ADDED,
    DEVICE_REMOVED,
    DEVICE_ONLINE,
    DEVICE_OFFLINE,
    
    DEVICE_NBUS_CREATED,
    DEVICE_NBUS_BROKEN,
    DEVICE_NBUS_DUPLICATE_SESSION,

    DEVICE_PACKET_PING,
    DEVICE_PACKET_UBUS;

    private final static Map<String, SystemRecordType> __map = 
            new ConcurrentHashMap<String, SystemRecordType>();
    
    static
    {
        for(SystemRecordType systemRecordType : SystemRecordType.values())
        {
            __map.put(systemRecordType.name(), systemRecordType);
        }
    }
    
    public static boolean contains(String value)
    {
        return __map.containsKey(value);
    }

    public static SystemRecordType fromString(String value)
    {
        return __map.get(value);
    }    }
