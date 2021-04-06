package com.sercomm.openfire.plugin.data.ubus;

import java.util.HashMap;
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
        public HashMap<String, Resource> Resources;
    }
    
    public static class Resource
    {
        public Integer Total;
        public Integer Free;
        public Double Usage;
    }    
}
