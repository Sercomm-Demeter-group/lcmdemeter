package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum BatchCommand
{
    INSTALL_START(1),
    INSTALL(2),
    UNINSTALL(3),
    START(4),
    STOP(5);

    private static Map<Integer, BatchCommand> map =
        new ConcurrentHashMap<>();
    static
    {
        for(BatchCommand object : BatchCommand.values())
        {
            map.put(object.intValue(), object);
        }
    }

    private Integer value;
    private BatchCommand(Integer value)
    {
        this.value = value;
    }

    public int intValue()
    {
        return this.value;
    }

    public static BatchCommand fromValue(Integer value)
    {
        return map.get(value);
    }
}
