package com.sercomm.openfire.plugin.prop;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum DeviceProperty
{
    SERCOMM_DEVICE_TYPE("sercomm.device.type"),
    SERCOMM_DEVICE_MODEL_NAME("sercomm.device.model.name"),
    SERCOMM_DEVICE_FIRMWARE_VERSION("sercomm.device.firmware.version"),
    SERCOMM_DEVICE_PLATFORM("sercomm.device.platform"),
    SERCOMM_DEVICE_STATE("sercomm.device.state"),
    SERCOMM_DEVICE_LAST_ONLINE_TIME("sercomm.device.last.online.time"),
    SERCOMM_DEVICE_LAST_OFFLINE_TIME("sercomm.device.last.offline.time"),
    SERCOMM_DEVICE_COMPANY("sercomm.device.company"),
    SERCOMM_DEVICE_CUSTOM_NAME("sercomm.device.custom.name"),
    SERCOMM_DEVICE_ENABLE("sercomm.device.enable"),
    SERCOMM_DEVICE_PROTOCOL_VERSION("sercomm.device.protocol.version");
    
    private static Map<String, DeviceProperty> __map = 
            new ConcurrentHashMap<String, DeviceProperty>();
    static
    {
        for(DeviceProperty deviceProperty : DeviceProperty.values())
        {
            __map.put(deviceProperty.toString(), deviceProperty);
        }
    }
    
    private String value;
    private DeviceProperty(String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return this.value;
    }
    
    public static DeviceProperty fromString(String value)
    {
        return __map.get(value);
    }
}
