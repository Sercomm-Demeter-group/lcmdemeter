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
        }
        catch(Throwable t)
        {
            Log.write().error(t.getMessage(), t);
        }
	}
}

