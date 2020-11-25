package com.sercomm.openfire.plugin.util;

import java.util.List;
import java.util.Map;

import com.sercomm.commons.util.Json;

public class ArgumentUtil
{
    public static Object get(List<Object> arguments, int idx, Class<?> clazz, Object defaultValue)
    {
        if(arguments.size() < idx + 1)
        {
            return defaultValue;
        }
        
        Object argeument = arguments.get(idx);
        return Json.mapper().convertValue(argeument, clazz);
    }
    
    public static String get(List<Object> arguments, int idx, String defaultValue)
    {
        if(arguments.size() < idx + 1)
        {
            return defaultValue;
        }
        
        return (String) arguments.get(idx);
    }
    
    public static Byte get(List<Object> arguments, int idx, Byte defaultValue)
    {
        if(arguments.size() < idx + 1)
        {
            return defaultValue;
        }
        
        return (Byte) arguments.get(idx);
    }    
    
    public static Short get(List<Object> arguments, int idx, Short defaultValue)
    {
        if(arguments.size() < idx + 1)
        {
            return defaultValue;
        }
        
        return (Short) arguments.get(idx);
    }    
    
    public static Integer get(List<Object> arguments, int idx, Integer defaultValue)
    {
        if(arguments.size() < idx + 1)
        {
            return defaultValue;
        }
        
        return (Integer) arguments.get(idx);
    }    
    
    public static Long get(List<Object> arguments, int idx, Long defaultValue)
    {
        if(arguments.size() < idx + 1)
        {
            return defaultValue;
        }
        
        return (Long) arguments.get(idx);
    }    
    
    public static Float get(List<Object> arguments, int idx, Float defaultValue)
    {
        if(arguments.size() < idx + 1)
        {
            return defaultValue;
        }
        
        return (Float) arguments.get(idx);
    }    
    
    public static Double get(List<Object> arguments, int idx, Double defaultValue)
    {
        if(arguments.size() < idx + 1)
        {
            return defaultValue;
        }
        
        return (Double) arguments.get(idx);
    }

    public static Map<?,?> get(List<Object> arguments, int idx, Map<String,Object> defaultValue)
    {
        if(arguments.size() < idx + 1)
        {
            return defaultValue;
        }
        
        return (Map<?,?>) arguments.get(idx);
    }
}
