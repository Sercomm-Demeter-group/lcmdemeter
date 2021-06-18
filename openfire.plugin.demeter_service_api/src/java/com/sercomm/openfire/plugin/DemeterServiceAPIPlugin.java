package com.sercomm.openfire.plugin;

import java.io.File;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemeterServiceAPIPlugin implements Plugin
{
    private static final Logger log = LoggerFactory.getLogger(DemeterServiceAPIPlugin.class);

    public final static String NAME = "demeter_service_api";

    private final static int DEFAULT_PORT = 8080;
    private final HandlerList handlers = new HandlerList();
    private Server server;

    @Override
	public void initializePlugin(
	        PluginManager manager, 
	        File pluginDirectory)
	{
        try
        {
            EndUserManager.getInstance().initialize();
            ServiceSessionManager.getInstance().initialize();
            
            // Jersey 2.22.2
            ResourceConfig resourceConfig = new ResourceConfig();
            // register multi-part feature 
            resourceConfig.register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);

            // register API.v1 (services)
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v1.AuthAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v1.IdentityAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v1.AppAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v1.CatalogAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v1.FileAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v1.UsageAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v1.EventAPI.class);

            // register API.v1 (administrator services)
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.admin.AppAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.admin.CatalogAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.admin.DeviceAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.admin.CustomerAPI.class);

            // register API.v2
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.UMEiException.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.InternalErrorException.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.AuthAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.UserAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.UsersAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.DeviceModelAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.DeviceModelsAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.DevicesAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.AppAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.AppsAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.AppVersionAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.AppVersionsAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.BatchAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.BatchesAPI.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.api.v2.InstallationsAPI.class);

            // register filters
            resourceConfig.register(com.sercomm.openfire.plugin.service.filter.v1.AuthFilter.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.filter.v2.AuthFilter.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.filter.v1.PermissionFilter.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.filter.v2.PermissionFilter.class);
            resourceConfig.register(com.sercomm.openfire.plugin.service.filter.CORSFilter.class);
            
            // register exception mapper
            // create servlet container
            ServletContainer servletContainer = new ServletContainer(resourceConfig);
            ServletHolder holder = new ServletHolder(servletContainer);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            context.addServlet(holder, "/*");
            this.handlers.addHandler(context);
            
            this.startJetty();
        }
        catch(Throwable t)
        {
            log.error(t.getMessage(), t);
            
            try
            {
                this.stopJetty();
            }
            catch(Throwable ignored) {}
        }
	}

    @Override
	public void destroyPlugin()
	{
        try
        {
            ServiceSessionManager.getInstance().uninitialize();
            EndUserManager.getInstance().uninitialize();
            
            this.stopJetty();            
        }
        catch(Throwable t)
        {
            log.error(t.getMessage(), t);
        }
	}
    
    private void startJetty()
    throws Throwable
    {
        final QueuedThreadPool threadPool = new QueuedThreadPool(200);
        threadPool.setName(DemeterServiceAPIPlugin.class.getName());

        // Jetty 9.2.X server
        this.server = new Server(threadPool);
        // Jetty 9.2.X HTTP configuration
        final HttpConfiguration configuration = new HttpConfiguration();
        configuration.setSendServerVersion(false);
        // Jetty 9.2.X connector
        final ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(configuration));
        connector.setHost(null);
        connector.setPort(DEFAULT_PORT);
        
        // Jetty 9.2.X start
        this.server.addConnector(connector);
        this.server.setHandler(this.handlers);
        
        this.server.start();    
        this.handlers.start();
    }
    
    private void stopJetty()
    throws Throwable
    {
        this.handlers.stop();
        if(null != this.server && this.server.isStarted())
        {
            server.stop();
        }        
    }
}

