package com.sercomm.commons.util;

import org.apache.commons.lang.StringUtils;

public class XStringUtil extends StringUtils
{
    public final static String BLANK = "";
    public final static String ZERO = "0";
    public final static String NOTHING = "N/A";
    
    public final static String SUCCESS = "SUCCESS";
    public final static String FAILURE = "FAILURE";
    
    public static String defaultIfEmpty(String value, String defaultValue)
    {
        return StringUtils.defaultIfEmpty(value, defaultValue);
    }

    public static String replaceLast(String text, String regex, String replacement) 
    {
        return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }
}
