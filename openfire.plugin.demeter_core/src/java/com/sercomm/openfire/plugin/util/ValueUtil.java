package com.sercomm.openfire.plugin.util;

import com.sercomm.commons.util.XStringUtil;

public class ValueUtil
{
    // for call by reference
    public static class Value
    {
        public Object value;
    }
    
    public static boolean isModified(String oldValue, String newValue)
    {
        boolean isModified = false;
        do
        {
            if(null == oldValue && null != newValue)
            {
                isModified = true;
                break;
            }
            
            String oldVal = XStringUtil.defaultIfEmpty(oldValue, XStringUtil.BLANK);
            String newVal = XStringUtil.defaultIfEmpty(newValue, XStringUtil.BLANK);
            if(0 != oldVal.compareTo(newVal))
            {
                isModified = true;
                break;
            }
        }
        while(false);
        
        return isModified;
    }
    
    public static boolean isModified(Integer oldValue, Integer newValue)
    {
        return oldValue == newValue ? false : true;
    }

    public static boolean isModified(Long oldValue, Long newValue)
    {
        return oldValue == newValue ? false : true;
    }

    public static boolean isModified(Boolean oldValue, Boolean newValue)
    {
        return oldValue == newValue ? false : true;
    }    
}
