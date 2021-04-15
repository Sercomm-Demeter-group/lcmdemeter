package com.sercomm.openfire.plugin;

import java.io.File;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemeterC2CAPIPlugin implements Plugin
{
    private static final Logger log = LoggerFactory.getLogger(DemeterC2CAPIPlugin.class);
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
            log.error(t.getMessage(), t);
            
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
            log.error(t.getMessage(), t);
        }
	}
}

