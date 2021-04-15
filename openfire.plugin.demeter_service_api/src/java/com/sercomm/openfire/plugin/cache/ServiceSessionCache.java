package com.sercomm.openfire.plugin.cache;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jivesoftware.util.cache.CacheSizes;
import org.jivesoftware.util.cache.CannotCalculateSizeException;
import org.jivesoftware.util.cache.ExternalizableUtil;

import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.define.EndUserRole;

public class ServiceSessionCache implements CacheBase
{
    private String sessionId;
    private String userId;
    private String storedKey;
    private EndUserRole endUserRole;

    public ServiceSessionCache()
    {
    }
    
    public String getSessionId()
    {
        return this.sessionId;
    }
    
    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }
    
    public String getUserId()
    {
        return this.userId;
    }
    
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public String getStoredKey()
    {
        return this.storedKey;
    }
    
    public void setStoredKey(String storedKey)
    {
        this.storedKey = storedKey;
    }
    
    public EndUserRole getEndUserRole()
    {
        return this.endUserRole;
    }
    
    public void setEndUserRole(EndUserRole endUserRole)
    {
        this.endUserRole = endUserRole;
    }

    @Override
    public int getCachedSize()
        throws CannotCalculateSizeException
    {
        int size = 0;
        // overhead of object
        size += CacheSizes.sizeOfObject();
        // sessionId
        size += CacheSizes.sizeOfString(this.sessionId);
        // name
        size += CacheSizes.sizeOfString(this.userId);
        // storedKey
        size += CacheSizes.sizeOfString(this.storedKey);
        // endUserRole
        size += CacheSizes.sizeOfString(null == this.endUserRole ? XStringUtil.BLANK : this.endUserRole.toString());
        
        return size;
    }

    @Override
    public void writeExternal(
            ObjectOutput out)
        throws IOException
    {
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.sessionId);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.userId);
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.storedKey);
        ExternalizableUtil.getInstance().writeSafeUTF(out, null == this.endUserRole ? XStringUtil.BLANK : this.endUserRole.toString());
    }

    @Override
    public void readExternal(
            ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        this.sessionId = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.userId = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.storedKey = ExternalizableUtil.getInstance().readSafeUTF(in);
        this.endUserRole = EndUserRole.fromString(ExternalizableUtil.getInstance().readSafeUTF(in));
    }

    @Override
    public String getUID()
    {
        return this.sessionId;
    }

}
