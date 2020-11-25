package com.sercomm.openfire.plugin.define;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum StorageType
{
    LOCAL_FS,
    AWS_S3,
    AZURE_BLOB;
    
    private static Map<String, StorageType> __map = 
            new ConcurrentHashMap<String, StorageType>();
    static
    {
        for(StorageType storageType : StorageType.values())
        {
            __map.put(storageType.name(), storageType);
        }
    }

    public static StorageType fromString(String value)
    {
        return __map.get(value);
    }

}
