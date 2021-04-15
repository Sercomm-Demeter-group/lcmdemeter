package com.sercomm.openfire.plugin;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.http.HttpBindManager;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.LocalSession;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.openfire.plugin.websocket.v0.DeviceWebSocket;

public class WebSocketEntryManager extends ManagerBase
{
    private static final Logger log = LoggerFactory.getLogger(WebSocketEntryManager.class);

    private ServletContextHandler contextHandler;

    private WebSocketEntryManager()
    {
    }
 
    private static class EntryManagerContainer
    {
        private final static WebSocketEntryManager instance = new WebSocketEntryManager();
    }
    
    public static WebSocketEntryManager getInstance()
    {
        return EntryManagerContainer.instance;
    }
    
    @Override
    protected void onInitialize()
    {
        if(Boolean.valueOf(JiveGlobals.getBooleanProperty(HttpBindManager.HTTP_BIND_ENABLED, true))) 
        {
            try 
            {
                this.contextHandler = new ServletContextHandler(null, "/cpe", ServletContextHandler.SESSIONS);
                this.contextHandler.setAllowNullPathInfo(true);
                this.contextHandler.addServlet(new ServletHolder(
                    new com.sercomm.openfire.plugin.websocket.v0.OpenfireWebSocketServlet()), "/v0/entry");
                this.contextHandler.addServlet(new ServletHolder(
                    new com.sercomm.openfire.plugin.websocket.v1.OpenfireWebSocketServlet()), "/v1/entry");
                
                HttpBindManager.getInstance().addJettyHandler(this.contextHandler);
            } 
            catch(Throwable t) 
            {
                log.error("FAILED TO START 'sercomm_websocket' PLUGIN: ", t.getMessage());
            }
        } 
        else 
        {
            log.error("FAILED TO START 'sercomm_websocket' PLUGIN: " + "http-bind is disabled");
        }
    }

    @Override
    protected void onUninitialize()
    {
        // terminate any active sessions
        SessionManager manager = XMPPServer.getInstance().getSessionManager();
        for(ClientSession session : manager.getSessions()) 
        {
            if(session instanceof LocalSession) 
            {
                Object sercommId = ((LocalSession) session).getSessionData(DeviceWebSocket.SESSION_SERCOMM);
                if(sercommId != null && (Boolean) sercommId) 
                {
                    session.close();
                }
            }
        }
            
        HttpBindManager.getInstance().removeJettyHandler(this.contextHandler);
        this.contextHandler = null;
    }
}
