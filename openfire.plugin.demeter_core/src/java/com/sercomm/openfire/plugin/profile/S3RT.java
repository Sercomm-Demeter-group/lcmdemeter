package com.sercomm.openfire.plugin.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.SystemProperties;
import com.sercomm.openfire.plugin.UbusManager;
import com.sercomm.openfire.plugin.define.UbusMethod;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.util.UbusUtil;

public class S3RT implements IProfile
{
    @Override
    public Map<?,?> getUsage(String serial, String mac, String appName)
    throws Throwable
    {
        UbusManager.getInstance().fire(
            serial, 
            mac, 
            "Get", 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", appName), 
            XStringUtil.BLANK, 
            10 * 1000L);
        
        Thread.sleep(1000L);

        Map<?,?> dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            "Get", 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", appName), 
            XStringUtil.BLANK, 
            10 * 1000L);

        return dataModel;
    }

    @Override
    public Map<?,?> installApp(String serial, String mac, String appName, Map<String, Object> payload)
    throws Throwable
    {
        Map<?,?> dataModel;
        com.sercomm.openfire.plugin.data.ubus.Response response;

        // 1st: check if its container exists
        try
        {
            UbusManager.getInstance().fire(
                serial, 
                mac, 
                "Get",
                UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", appName),
                XStringUtil.BLANK, 
                10 * 1000L);
        }
        catch(Throwable t)
        {
            // make payload string
            com.sercomm.openfire.plugin.data.ubus.Credentials credentials = 
                    new com.sercomm.openfire.plugin.data.ubus.Credentials();
            credentials.Username = XStringUtil.BLANK;
            credentials.Password = XStringUtil.BLANK;
            
            com.sercomm.openfire.plugin.data.ubus.Source source =
                    new com.sercomm.openfire.plugin.data.ubus.Source();

            source.Protocol = SystemProperties.getInstance().getStorageScheme();
            source.Address = SystemProperties.getInstance().getHostServiceAPI().getAddress();
            source.Port = SystemProperties.getInstance().getHostServiceAPI().getPort().toString();
            // must be end with its filename
            source.Resource = String.format("/api/v1/files/lxc_conf/%s/%s.xml", NameRule.formatDeviceName(serial, mac),  appName);
            source.Credentials = credentials;

            Map<String,Object> payload1 = new HashMap<>();
            payload1.put("Id", appName);
            payload1.put("Name", appName);
            payload1.put("Enabled", Boolean.TRUE);
            payload1.put("Retry", 3);
            payload1.put("Source", source);
            
            // create its container
            dataModel = UbusManager.getInstance().fire(
                serial,
                mac,
                "Add",
                UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments"),
                Json.build(payload1),
                10 * 1000L);

            response = Json.mapper().convertValue(
                dataModel, com.sercomm.openfire.plugin.data.ubus.Response.class);
            
            if("OK".compareTo(response.Header.Name) != 0)
            {
                throw new DemeterException("UBUS RETURNED " + response.Header.Name);
            }
        }
        
        // install the App into its container
        dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            "Install",
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", appName, "Packages"),
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
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", appName, "Packages", appName),
            Json.build(payload), 
            10 * 1000L);

        return dataModel;
    }

    @Override
    public Map<?,?> uninstallApp(String serial, String mac, String appName)
    throws Throwable
    {
        Map<?,?> dataModel;
        com.sercomm.openfire.plugin.data.ubus.Response response;

        dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            "Delete", 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", appName, "Packages", appName),
            XStringUtil.BLANK, 
            10 * 1000L);
        
        response = Json.mapper().convertValue(
            dataModel, com.sercomm.openfire.plugin.data.ubus.Response.class);
        
        if("OK".compareTo(response.Header.Name) != 0)
        {
            throw new DemeterException("UBUS RETURNED " + response.Header.Name);
        }

        dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            "Delete", 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", appName),
            XStringUtil.BLANK, 
            10 * 1000L);

        return dataModel;
    }

    @Override
    public List<Map<?,?>> getInstalledApps(String serial, String mac) 
    throws Throwable
    {
        List<Map<?,?>> dataModels = new ArrayList<Map<?,?>>();
        Map<?,?> dataModel;
        com.sercomm.openfire.plugin.data.ubus.Response response;
        
        // 1st: query all execution environments
        UbusManager.getInstance().fire(
            serial, 
            mac, 
            "List", 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments"),
            XStringUtil.BLANK, 
            10 * 1000L);
        
        Thread.sleep(1000L);
        
        dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            "List", 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments"),
            XStringUtil.BLANK, 
            10 * 1000L);

        response = Json.mapper().convertValue(
            dataModel, com.sercomm.openfire.plugin.data.ubus.Response.class);

        if("OK".compareTo(response.Header.Name) != 0)
        {
            throw new DemeterException("UBUS RETURNED " + response.Header.Name);
        }
        
        com.sercomm.openfire.plugin.data.ubus.ExecutionEnvironments executionEnvironments = 
            Json.mapper().convertValue(
                response.Body,
                com.sercomm.openfire.plugin.data.ubus.ExecutionEnvironments.class);
        
        // 2nd: iterator all execution environments and query their installed App
        for(com.sercomm.openfire.plugin.data.ubus.ExecutionEnvironments.ExecutionEnvironment executionEnvironment : executionEnvironments.List)
        {
            dataModel = UbusManager.getInstance().fire(
                serial, 
                mac, 
                "List", 
                UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", executionEnvironment.Id, "Packages"),
                XStringUtil.BLANK, 
                10 * 1000L);
            
            dataModels.add(dataModel);
        }
                
        return dataModels;
    }

    @Override
    public Map<?, ?> getInstalledApp(String serial, String mac, String appName) 
    throws Throwable
    {
        Map<?,?> dataModel = UbusManager.getInstance().fire(
            serial, 
            mac, 
            "Get", 
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", appName, "Packages", appName),
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
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", appName, "Packages", appName),
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
            UbusUtil.buildPath("Services", "Management", "LCM", "ExecutionEnvironments", appName, "Packages", appName),
            XStringUtil.BLANK, 
            10 * 1000L);                        

        return dataModel;
    }

    @Override
    public String generateContainerConfiguration(String serial, String mac, String appName)
        throws Throwable
    {
        return "<domain type='lxc'><name>libvirt_container1</name><uuid>902b56ed-969c-458e-8b55-58daf5ae97b3</uuid><memory unit='KiB'>524288</memory><currentMemory unit='KiB'>524288</currentMemory><vcpu placement='static'>1</vcpu><os><type arch='x86_64'>exe</type><init>/sbin/init</init></os><features><capabilities policy='allow'></capabilities></features><clock offset='utc'/><on_poweroff>destroy</on_poweroff><on_reboot>restart</on_reboot><on_crash>destroy</on_crash><devices><emulator>/usr/local/libexec/libvirt_lxc</emulator><filesystem type='mount' accessmode='passthrough'><source dir='/root/container'/><target dir='/'/></filesystem><interface type='bridge'><mac address='00:17:4e:9f:36:f8'/><source bridge='lxcbr0'/><link state='up'/></interface><console type='pty' /></devices></domain>";
    }
}
