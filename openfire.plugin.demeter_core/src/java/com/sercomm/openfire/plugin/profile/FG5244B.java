package com.sercomm.openfire.plugin.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.UbusManager;
import com.sercomm.openfire.plugin.define.UbusMethod;
import com.sercomm.openfire.plugin.util.UbusUtil;

public class FG5244B implements IProfile
{
    private static String convertEnvironmentName(String appName)
    {
        return "Default";
    }
    
    @Override
    public Map<?,?> getUsage(String serial, String mac, String appName)
    throws Throwable
    {
        UbusManager.getInstance().fire(
            serial, 
            mac, 
            "Get", 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", convertEnvironmentName(XStringUtil.BLANK)), 
            XStringUtil.BLANK, 
            10 * 1000L);
        
        Thread.sleep(1000L);
        
        Map<?,?> dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            "Get", 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", convertEnvironmentName(XStringUtil.BLANK)), 
            XStringUtil.BLANK, 
            10 * 1000L);
        
        return dataModel;
    }

    @Override
    public Map<?,?> installApp(String serial, String mac, String appName, Map<String, Object> payload)
    throws Throwable
    {
        Map<?,?> dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            UbusMethod.INSTALL.toString(),
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", convertEnvironmentName(appName), "Packages"),
            Json.build(payload), 
            10 * 1000L);

        return dataModel;
    }

    @Override
    public Map<?,?> patchApp(String serial, String mac, String appName, Map<String, Object> payload) 
    throws Throwable
    {
        Map<?,?> dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            UbusMethod.UPDATE.toString(),
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", convertEnvironmentName(appName), "Packages", appName),
            Json.build(payload), 
            10 * 1000L);

        return dataModel;
    }

    @Override
    public Map<?,?> uninstallApp(String serial, String mac, String appName)
    throws Throwable
    {
        Map<?,?> dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            UbusMethod.DELETE.toString(), 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", convertEnvironmentName(appName), "Packages", appName),
            XStringUtil.BLANK, 
            10 * 1000L);
        
        return dataModel;
    }

    @Override
    public List<Map<?,?>> getInstalledApps(String serial, String mac) 
    throws Throwable
    {
        List<Map<?,?>> dataModels = new ArrayList<Map<?,?>>();
        
        Map<?,?> dataModel1 = UbusManager.getInstance().fire(
            serial, 
            mac, 
            "get", 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", convertEnvironmentName(XStringUtil.BLANK), "Packages"),
            XStringUtil.BLANK, 
            10 * 1000L);
        
        dataModels.add(dataModel1);

        return dataModels;
    }

    @Override
    public Map<?, ?> getInstalledApp(String serial, String mac, String appName) 
    throws Throwable
    {
        Map<?,?> dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            "get", 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", convertEnvironmentName(appName), "Packages", appName),
            XStringUtil.BLANK, 
            10 * 1000L);
        
        return dataModel;
    }

    @Override
    public Map<?,?> startApp(String serial, String mac, String appName)
    throws Throwable
    {
        Map<?,?> dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            UbusMethod.START.toString(), 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", convertEnvironmentName(appName), "Packages", appName),
            XStringUtil.BLANK, 
            10 * 1000L);                        

        return dataModel;
    }

    @Override
    public Map<?,?> stopApp(String serial, String mac, String appName)
    throws Throwable
    {
        Map<?,?> dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            UbusMethod.STOP.toString(), 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", convertEnvironmentName(appName), "Packages", appName),
            XStringUtil.BLANK, 
            10 * 1000L);                        

        return dataModel;
    }

    @Override
    public String generateContainerConfiguration(String serial, String mac, String appName)
        throws Throwable
    {
        return XStringUtil.BLANK;
    }
}
