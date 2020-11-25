package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum SystemRecordCategory
{
    INFO(0),
    WARN(1),
    ERROR(2);
    
    private final static Map<Integer, SystemRecordCategory> __map = 
            new ConcurrentHashMap<Integer, SystemRecordCategory>();
    
    static
    {
        for(SystemRecordCategory systemRecordCategory : SystemRecordCategory.values())
        {
            __map.put(systemRecordCategory.intValue(), systemRecordCategory);
        }
    }
    
    private final Integer value;
    private SystemRecordCategory(Integer value)
    {
        this.value = value;
    }

    public int intValue()
    {
        return this.value;
    }
    
    public static SystemRecordCategory fromInt(Integer value)
    {
        return __map.get(value);
    }    
}
