package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum UbusMethod
{
    LIST("List"),
    INSTALL("Install"),
    UPDATE("Update"),
    DELETE("Delete"),
    START("Start"),
    STOP("Stop");
    
    private static Map<String, UbusMethod> __map = 
            new ConcurrentHashMap<String, UbusMethod>();
    static
    {
        for(UbusMethod method : UbusMethod.values())
        {
            __map.put(method.toString(), method);
        }
    }

    private String value;
    private UbusMethod(String value)
    {
        this.value = value;
    }
    
    @Override
    public String toString()
    {
        return this.value;
    }
    
    public static UbusMethod fromString(String value)
    {
        return __map.get(value);
    }
}
