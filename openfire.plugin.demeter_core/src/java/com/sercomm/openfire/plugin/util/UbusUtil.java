package com.sercomm.openfire.plugin.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class UbusUtil
{
    public static String buildPath(String ...tokens)
    {
        List<String> array = Arrays.asList(tokens);
        Iterator<String> iterator = array.iterator();
        
        StringBuilder builder = new StringBuilder();
        while(iterator.hasNext())
        {
            String token = iterator.next();
            builder.append(token);
            
            if(iterator.hasNext())
            {
                builder.append(".");
            }
        }
        
        return builder.toString();
    }
}
