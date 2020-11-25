package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum OwnershipType
{
    OWNED,
    SHARED;
    
    private static Map<String, OwnershipType> __map = 
            new ConcurrentHashMap<String, OwnershipType>();
    static
    {
        for(OwnershipType ownershipType : OwnershipType.values())
        {
            __map.put(ownershipType.name(), ownershipType);
        }
    }

    public static OwnershipType fromString(String value)
    {
        return __map.get(value);
    }
    
}
