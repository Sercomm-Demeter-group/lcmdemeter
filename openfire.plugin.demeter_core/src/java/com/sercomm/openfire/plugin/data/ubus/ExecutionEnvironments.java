package com.sercomm.openfire.plugin.data.ubus;

import java.util.List;

public class ExecutionEnvironments
{
    public List<ExecutionEnvironment> List;
    public Integer Limit;
    public Integer Offset;
    
    public static class ExecutionEnvironment
    {
        public String Id;
        public String Name;
        public Boolean Enabled;
        public String Version;
        public String Vendor;
        public String Type;
        public String Status;
        public List<Resource> Resources;
    }
    
    public static class Resource
    {
        public Memory Memory;
        public Storage Storage;
        public CPU CPU;
    }
    
    public static class Memory
    {
        public Integer Total;
        public Integer Free;
        public Double Usage;
    }
    
    public static class Storage
    {
        public Integer Total;
        public Integer Free;
        public Double Usage;
    }
    
    public static class CPU
    {
        public String Usage;
    }
}
