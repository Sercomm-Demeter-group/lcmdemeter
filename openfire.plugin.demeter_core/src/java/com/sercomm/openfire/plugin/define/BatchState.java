package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum BatchState 
{
    PENDING("PENDING"),
    EXECUTING("EXECUTING"),
    PAUSING("PAUSING"),
    PAUSED("PAUSED"),
    TERMINATING("TERMINATING"),
    TERMINATED("TERMINATED"),
    DONE("DONE");

    private static Map<String, BatchState> map = 
            new ConcurrentHashMap<String, BatchState>();
    static
    {
        for(BatchState object : BatchState.values())
        {
            map.put(object.toString(), object);
        }
    }

    private String value;
    private BatchState(String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return this.value;
    }

    public static BatchState fromString(String value)
    {
        return map.get(value);
    }
}
