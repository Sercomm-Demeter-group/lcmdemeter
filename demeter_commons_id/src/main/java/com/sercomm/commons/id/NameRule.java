package com.sercomm.commons.id;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameRule
{
    private final static Pattern PATTERN_DEVICE =
            Pattern.compile("[0-9a-fA-F]{1,}-[0-9a-fA-F]{12}", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_EMAIL = 
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static final String BLANK = "";
    
    public static String formatDeviceName(
            String serial,
            String mac)
    {
        StringBuilder builder = new StringBuilder();       
        builder.append(serial.toLowerCase()).append("-").append(mac.toLowerCase());
        
        return builder.toString();
    }

    public static Boolean isDevice(String nodeName)
    {
        if(isBlank(nodeName))
        {
            return false;
        }
        
        Matcher matcher = PATTERN_DEVICE.matcher(nodeName);
        return matcher.matches();
    }

    public static boolean isEmail(String email)
    {
        if(isBlank(email))
        {
            return false;
        }

        Matcher matcher = PATTERN_EMAIL.matcher(email);
        return matcher.find();
    }    

    public static String toDeviceSerial(String nodeName)
    {
        String[] tokens = nodeName.split("-");
        if(tokens.length != 2)
        {
            return BLANK;
        }
        
        return tokens[0].toUpperCase();
    }
    
    public static String toDeviceMac(String nodeName)
    {
        String[] tokens = nodeName.split("-");
        if(tokens.length != 2)
        {
            return BLANK;
        }

        return tokens[1].toUpperCase();
    }
    
    public static String formatMac(String mac)
    {
        StringBuilder builder = new StringBuilder();
        
        int position = 0;
        for(int idx = 0; idx < 6; idx++)
        {
            builder.append(mac.substring(position, position + 2));                
            position = position + 2;
            if(position >= mac.length())
            {
                break;
            }
            
            builder.append(":");
        }
        
        return builder.toString();
    }
    
    /**
     * <p>Checks if a String is whitespace, empty ("") or null.</p>
     *
     * @param str  the String to check, may be null
     * @return <code>true</code> if the String is null, empty or whitespace
     * @since 2.0
     */
    private static boolean isBlank(String str) 
    {
        int strLen;
        if(str == null || (strLen = str.length()) == 0) 
        {
            return true;
        }

        for(int i = 0; i < strLen; i++) 
        {
            if((Character.isWhitespace(str.charAt(i)) == false)) 
            {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>Checks if a String is not empty (""), not null and not whitespace only.</p>
     *
     * @param str  the String to check, may be null
     * @return <code>true</code> if the String is
     *  not empty and not null and not whitespace
     * @since 2.0
     */
    public static boolean isNotBlank(String str) 
    {
        return !isBlank(str);
    }
}
