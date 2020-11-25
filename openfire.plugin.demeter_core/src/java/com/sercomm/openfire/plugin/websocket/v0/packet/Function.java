package com.sercomm.openfire.plugin.websocket.v0.packet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum Function
{
    F_PING,
    F_AUTHENTICATE,
    F_AUTHENTICATE_V2,
    F_REBOOT,
    F_UPGRADE,
    F_UBUS;
    
    private static Map<String, Function> __map = 
            new ConcurrentHashMap<String, Function>();
    static
    {
        for(Function function : Function.values())
        {
            __map.put(function.name(), function);
        }
    }

    public static Function fromString(String value)
    {
        return __map.get(value);
    }
}
