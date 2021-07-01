package com.sercomm.openfire.plugin;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer
{
    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

    public static final String SERVICE_NAME = "demeter.core";
    public static final int DEFAULT_PORT = 8090;

    private Server server;
    private int port;
    private final HandlerList handlers = new HandlerList();

    public HttpServer()
    {
        this.port = DEFAULT_PORT;
    }

    public HttpServer(int port)
    {
        this.port = port;
    }

    public void start()
    {
        try
        {
            // setup Jetty 9.2.X
            ResourceConfig resourceConfig = new ResourceConfig();       
            // register resources: filters
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.filter.CORSFilter.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.filter.HeaderFilter.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.filter.ResourceFilter.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.filter.AuthenticationFilter.class);
            // register resources: exceptions
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.exception.UMEiException.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.exception.InternalErrorException.class);
            // register resources: API
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.PostEchoAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.user.PostUserAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.ubus.PostUbusAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.app.GetAppsAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.app.GetAppAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.app.GetAppByNameAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.device.GetDevicesAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.device.GetDeviceAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.device.InstallAppAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.device.UninstallAppAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.device.UpdateAppAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.device.GetInstalledAppAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.device.GetInstalledAppsAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.device.StartAppAPI.class);
            resourceConfig.register(
                com.sercomm.openfire.plugin.c2c.api.v1.device.StopAppAPI.class);
            
            ServletContainer servletContainer = new ServletContainer(resourceConfig);
            ServletHolder holder = new ServletHolder(servletContainer);
            
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            context.addServlet(holder, "/*");
            
            this.handlers.addHandler(context);
            
            final QueuedThreadPool threadPool = new QueuedThreadPool(200);
            threadPool.setName(getClass().getName());

            // Jetty 9.2.X server
            this.server = new Server(threadPool);
            // Jetty 9.2.X HTTP configuration
            final HttpConfiguration configuration = new HttpConfiguration();
            configuration.setSendServerVersion(false);
            // Jetty 9.2.X connector
            final ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(configuration));
            connector.setHost(null);
            connector.setPort(this.port);

            // Jetty 9.2.X start
            this.server.addConnector(connector);
            this.server.setHandler(this.handlers);
            
            this.server.start();
            this.handlers.start();

            log.info("(); SERVER {} STARTED", SERVICE_NAME);
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }        
    }

    public void stop()
    {
        try
        {
            this.handlers.stop();
            
            if(null != this.server)
            {
                if(false == this.server.isStopped() &&
                   false == this.server.isStopping())
                {
                    this.server.stop();
                }
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }

        log.info("(); SERVER {} STOPPED", SERVICE_NAME);
    }
}
