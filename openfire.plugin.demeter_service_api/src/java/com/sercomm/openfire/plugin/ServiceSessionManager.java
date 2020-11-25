package com.sercomm.openfire.plugin;

import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;

public class ServiceSessionManager extends ManagerBase 
{
    private final static String CACHE_NAME = "Demeter Service Session Caches";
    private Cache<String, ServiceSessionCache> caches;
    
    private static class ServiceSessionManagerContainer
    {
        private final static ServiceSessionManager instance = new ServiceSessionManager();
    }
    
    private ServiceSessionManager()
    {
    }
    
    public static ServiceSessionManager getInstance()
    {
        return ServiceSessionManagerContainer.instance;
    }
    
    @SuppressWarnings("all")
    @Override
    protected void onInitialize()
    {
        this.caches = CacheFactory.createCache(CACHE_NAME);
        this.caches.setMaxCacheSize(-1);
        this.caches.setMaxLifetime(1 * 60 * 60 * 1000L); // 1 hour
    }
    @Override
    protected void onUninitialize()
    {
    }

    public ServiceSessionCache getSession(String sessionId)
    {
        return this.caches.get(sessionId);
    }
    
    public void updateSession(ServiceSessionCache cache)
    {
        this.caches.put(cache.getSessionId(), cache);
    }
}
