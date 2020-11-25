package com.sercomm.openfire.plugin.cache;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jivesoftware.util.cache.CacheSizes;
import org.jivesoftware.util.cache.CannotCalculateSizeException;
import org.jivesoftware.util.cache.ExternalizableUtil;

import com.sercomm.common.util.Algorithm;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.define.OwnershipType;

public class OwnershipCache implements CacheBase
{
    private String serial;
    private String mac;
    private String userId;
    private OwnershipType ownershipType;
    private Long creationTime;
    
    public OwnershipCache()
    {
    }
    
    public String getSerial()
    {
        return serial;
    }

    public void setSerial(String serial)
    {
        this.serial = serial;
    }

    public String getMac()
    {
        return mac;
    }

    public void setMac(String mac)
    {
        this.mac = mac;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public OwnershipType getOwnershipType()
    {
        return ownershipType;
    }

    public void setOwnershipType(OwnershipType ownershipType)
    {
        this.ownershipType = ownershipType;
    }

    public Long getCreationTime()
    {
        return creationTime;
    }

    public void setCreationTime(Long creationTime)
    {
        this.creationTime = creationTime;
    }

    @Override
    public int getCachedSize() throws CannotCalculateSizeException
    {
        int size = 0;
        // overhead of object
        size += CacheSizes.sizeOfObject();
        // serial
        size += CacheSizes.sizeOfString(this.serial);
        // mac
        size += CacheSizes.sizeOfString(this.mac);
        // userId
        size += CacheSizes.sizeOfString(this.userId);
        // ownershipType
        size += CacheSizes.sizeOfString(null == this.ownershipType ? XStringUtil.BLANK : this.ownershipType.name());
        // creationTime
        size += CacheSizes.sizeOfLong();
        
        return size;
    }
    
    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.serial);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.mac);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.userId);
        ExternalizableUtil.getInstance().writeSafeUTF(out, null == this.ownershipType ? XStringUtil.BLANK : this.ownershipType.name());
        ExternalizableUtil.getInstance().writeLong(out, this.creationTime);
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.serial = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.mac = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.userId = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.ownershipType = OwnershipType.fromString(ExternalizableUtil.getInstance().readSafeUTF(in));
        this.creationTime = ExternalizableUtil.getInstance().readLong(in);
    }
    
    @Override
    public String getUID()
    {
        return Algorithm.md5(serial + mac + userId);
    }
}
