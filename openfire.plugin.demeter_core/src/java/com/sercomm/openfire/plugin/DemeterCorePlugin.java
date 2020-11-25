package com.sercomm.openfire.plugin;

import java.io.File;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.xmpp.component.ComponentManagerFactory;

import com.sercomm.commons.util.Log;
import com.sercomm.openfire.plugin.component.DeviceComponent;

public class DemeterCorePlugin implements Plugin
{
    private final DeviceComponent demeterComponent = new DeviceComponent();

    public DemeterCorePlugin()
    {
    }
    
    @Override
    public void initializePlugin(
            PluginManager manager,
            File pluginDirectory)
    {
        Log.initialize("demeter", "10MB", 10);

        // add system properties listener
        PropertyEventDispatcher.addListener(SystemProperties.getInstance());

        try
        {
            ComponentManagerFactory.getComponentManager().addComponent(
                DeviceComponent.NAME, 
                this.demeterComponent);

            SystemRecordManager.getInstance().initialize();
            AppEventManager.getInstance().initialize();
            WebSocketEntryManager.getInstance().initialize();
            DeviceManager.getInstance().initialize();
            //StanzaManager.getInstance().initialize();
            UbusManager.getInstance().initialize();
            AppCatalogManager.getInstance().initialize();
            AppManager.getInstance().initialize();
            OwnershipManager.getInstance().initialize();
            
            Log.write().info("({})={}", this.getClass().getName(), " LOADED");
        }
        catch(Throwable t)
        {
            Log.write().error(t.getMessage(), t);
        }
    }

    @Override
    public void destroyPlugin()
    {
        // remove system properties listener
        PropertyEventDispatcher.removeListener(SystemProperties.getInstance());
        
        try
        {
            OwnershipManager.getInstance().uninitialize();
            AppCatalogManager.getInstance().uninitialize();
            AppManager.getInstance().uninitialize();
            UbusManager.getInstance().uninitialize();
            //StanzaManager.getInstance().uninitialize();
            DeviceManager.getInstance().uninitialize();
            WebSocketEntryManager.getInstance().uninitialize();
            AppEventManager.getInstance().uninitialize();
            SystemRecordManager.getInstance().uninitialize();
            
            ComponentManagerFactory.getComponentManager().removeComponent(
                DeviceComponent.NAME);
         }
        catch(Throwable t)
        {
            Log.write().error(t.getMessage(), t);
        }        
    }
}
