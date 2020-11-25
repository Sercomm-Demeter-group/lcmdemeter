package com.sercomm.openfire.plugin.websocket.v0.packet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum Type
{
    T_REQUEST,
    T_RESULT,
    T_ERROR,
    T_MESSAGE;

    private static Map<String, Type> __map = 
            new ConcurrentHashMap<String, Type>();
    static
    {
        for(Type type : Type.values())
        {
            __map.put(type.name(), type);
        }
    }
        
    public static Type fromString(String value)
    {
        return __map.get(value);
    }
}

