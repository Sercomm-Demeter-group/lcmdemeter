package com.sercomm.openfire.plugin.data.ubus;

import java.util.List;

public class Packages
{
    public List<Package> List;
    public Integer Limit;
    public Integer Offset;
    
    public static class Package
    {
        public String Id;
        public String UUID;
        public String Name;
        public String Description;
        public Boolean Enabled;
        public Source Source;
        public Resources Resources;
        public String Section;
        public String Vendor;
        public String Version;
        public List<String> Dependencies;
        public String License;
        public String Architecture;
        public String Status;
        public Install Install;
        
        public static class Source
        {
            public String Protocol;
            public String Address;
            public String Port;
            public String Resource;
        }
        
        public static class Resources
        {
            public CPU CPU;
            public Memory Memory;
            public Storage Storage;
        }
        
        public static class CPU
        {
            public String Usage;
        }
        
        public static class Memory
        {
            public String Use;
        }
        
        public static class Storage
        {
            public String Size;
        }
        
        public static class Install
        {
            public String Timestamp;
            public String Size;
        }
    }
}
