package com.sercomm.openfire.plugin.prop;

import java.util.concurrent.ConcurrentHashMap;

public enum C2CPropertyEnum
{
    C2C_NOTIFY_KAFKA_CONFIG("sercomm.c2c.kafka.config");

    private static ConcurrentHashMap<String, C2CPropertyEnum> map =
            new ConcurrentHashMap<String, C2CPropertyEnum>();
    static
    {
        for(C2CPropertyEnum propertyEnum : C2CPropertyEnum.values())
        {
            map.put(propertyEnum.toString(), propertyEnum);
        }
    }
    
    private String value;
    private C2CPropertyEnum(String value)
    {
        this.value = value;
    }
    
    public static C2CPropertyEnum fromString(String value)
    {
        return map.get(value);
    }
    
    @Override
    public String toString()
    {
        return this.value;
    }
}
