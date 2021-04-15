package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum EndUserRole
{
    ADMIN("admin"),
    @Deprecated
    MEMBER("member"),
    EDITOR("editor"),
    OPERATOR("operator");

    private static Map<String, EndUserRole> map = 
            new ConcurrentHashMap<String, EndUserRole>();
    static
    {
        for(EndUserRole endUserRole : EndUserRole.values())
        {
            map.put(endUserRole.toString(), endUserRole);
        }
    }

    private String value;
    private EndUserRole(String value)
    {
        this.value = value;
    }
    
    @Override
    public String toString()
    {
        return this.value;
    }

    public static EndUserRole fromString(String value)
    {
        return map.get(value);
    }
}
