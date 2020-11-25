package com.sercomm.openfire.plugin.data.ubus;

public class Response
{
    public Header Header;
    public Object Body;
    
    public static class Header
    {
        public String Name;
    }    
}
