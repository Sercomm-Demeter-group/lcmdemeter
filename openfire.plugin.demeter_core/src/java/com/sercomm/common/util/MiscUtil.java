package com.sercomm.common.util;

import java.io.File;
import java.util.regex.Pattern;

public class MiscUtil
{
    public static String getFileExtension(File file) 
    {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
        {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        
        return "";
    }
    
    private final static Pattern IP_PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    public static boolean isIpAddress(final String ipAddress) 
    {
        return IP_PATTERN.matcher(ipAddress).matches();
    }
    
    public static String getMethodName(final int depth)
    {
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        return ste[ste.length - 1 - depth].getMethodName();
    }
}
