package com.sercomm.openfire.plugin.prop;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum EndUserProperty
{
    SERCOMM_ENDUSER_UPDATE_TIME("sercomm.enduser.updated.time");
    
    private static Map<String, EndUserProperty> __map = 
            new ConcurrentHashMap<String, EndUserProperty>();
    static
    {
        for(EndUserProperty cpeProperty : EndUserProperty.values())
        {
            __map.put(cpeProperty.toString(), cpeProperty);
        }
    }
    
    private String value;
    private EndUserProperty(String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return this.value;
    }
    
    public static EndUserProperty fromString(String value)
    {
        return __map.get(value);
    }
}
