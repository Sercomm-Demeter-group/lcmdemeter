package com.sercomm.openfire.plugin.c2c.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum Progress
{
    DELIVERED("delivered"),
    INSTALLING("installing"),
    COMPLETED("completed"),
    TIMEOUT("timeout"),
    FAIL("fail");
    
    private static Map<String, Progress> __map = 
            new ConcurrentHashMap<String, Progress>();
    static
    {
        for(Progress item : Progress.values())
        {
            __map.put(item.value, item);
        }
    }

    private String value;
    private Progress(String value)
    {
        this.value = value;
    }
    
    @Override
    public String toString()
    {
        return this.value;
    }
    
    public static Progress fromString(String value)
    {
        return __map.get(value);
    }
}
