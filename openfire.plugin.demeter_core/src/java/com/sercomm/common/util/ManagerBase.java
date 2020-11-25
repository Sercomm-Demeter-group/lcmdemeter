package com.sercomm.common.util;

public abstract class ManagerBase
{
    private Boolean isInitialized = false;
    
    public ManagerBase()
    {
        
    }
    
    public Boolean isInitialized()
    {
        return isInitialized;
    }
    
    public void initialize()
    {
        onInitialize();
        this.isInitialized = true;
    }
    
    public void uninitialize()
    {
        onUninitialize();
        this.isInitialized = false;
    }
    
    protected abstract void onInitialize();
    protected abstract void onUninitialize();
}
