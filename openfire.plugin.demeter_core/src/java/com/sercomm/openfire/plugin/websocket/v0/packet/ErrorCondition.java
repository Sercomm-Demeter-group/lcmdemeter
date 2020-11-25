package com.sercomm.openfire.plugin.websocket.v0.packet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ErrorCondition
{
    E_BAD_REQUEST,
    E_CONFLICT,
    E_FEATURE_NOT_IMPLEMENTED,
    E_FORBIDDEN,
    E_INTERNAL_SERVER_ERROR,
    E_ITEM_NOT_FOUND,
    E_NOT_ACCEPTABLE,
    E_NOT_ALLOWED,
    E_NOT_AUTHORIZED,
    E_REGISTRATION_REQUIRED,
    E_REMOTE_SERVER_NOT_AVAILABLE,
    E_REMOTE_SERVER_TIMEOUT,
    E_SERVICE_UNAVAILABLE,
    E_UNEXPECTED_CONDITION;
    
    private static Map<String, ErrorCondition> __map = 
            new ConcurrentHashMap<String, ErrorCondition>();
    static
    {
        for(ErrorCondition function : ErrorCondition.values())
        {
            __map.put(function.name(), function);
        }
    }

    public static ErrorCondition fromString(String value)
    {
        return __map.get(value);
    }
    
}
