package com.sercomm.openfire.plugin;

import java.io.File;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;

import com.sercomm.commons.util.Log;

public class DemeterC2CAPIPlugin implements Plugin
{
    private final HttpServer httpServer = new HttpServer();

    @Override
	public void initializePlugin(
	        PluginManager manager, 
	        File pluginDirectory)
	{
        try
        {
            PropertyManager.getInstance().initialize();
            C2CNotifyManager.getInstance().initialize();

            this.httpServer.start();
        }
        catch(Throwable t)
        {
            Log.write().error(t.getMessage(), t);
            
            this.httpServer.stop();
        }
	}

    @Override
	public void destroyPlugin()
	{
        try
        {
            this.httpServer.stop();

            C2CNotifyManager.getInstance().uninitialize();
            PropertyManager.getInstance().uninitialize();
        }
        catch(Throwable t)
        {
            Log.write().error(t.getMessage(), t);
        }
	}
}

