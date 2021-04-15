package com.sercomm.openfire.plugin.websocket.v0;

import java.text.MessageFormat;

import org.eclipse.jetty.websocket.common.extensions.compress.PerMessageDeflateExtension;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.LocalSession;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Servlet enables XMPP over WebSocket (RFC 7395) for Openfire.
 * 
 * The Jetty WebSocketServlet serves as a base class and enables easy integration into the
 * BOSH (http-bind) web context. Each WebSocket request received at the "/ws/" URI will be
 * forwarded to this plugin/servlet, which will in turn create a new {@link DeviceWebSocket}
 * for each new connection. 
 */
public class OpenfireWebSocketServlet extends WebSocketServlet 
{
    private static final long serialVersionUID = 7583369957545700231L;

    private static final Logger log = LoggerFactory.getLogger(OpenfireWebSocketServlet.class);

    @Override
    public void destroy()
    {
        // terminate any active websocket sessions
        SessionManager sm = XMPPServer.getInstance().getSessionManager();
        for (ClientSession session : sm.getSessions()) 
        {
            if (session instanceof LocalSession) 
            {
                Object ws = ((LocalSession) session).getSessionData("ws");
                if(ws != null && (Boolean) ws) 
                {
                    session.close();
                }
            }
        }

        super.destroy();
    }

    @Override
    public void configure(WebSocketServletFactory factory)
    {
        if(DeviceWebSocket.isCompressionEnabled()) 
        {
            factory.getExtensionFactory().register(
                "permessage-deflate", 
                PerMessageDeflateExtension.class);
        }
        
        final int messageSize = JiveGlobals.getIntProperty("xmpp.parser.buffer.size", 1048576);
        factory.getPolicy().setMaxTextMessageBufferSize(messageSize * 5);
        factory.getPolicy().setMaxTextMessageSize(messageSize);
        factory.setCreator(new WebSocketCreator() 
        {
            @Override
            public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse res)
            {
                try 
                {
                    return new DeviceWebSocket(
                        req.getHeader("X-Mutual-Auth"),
                        req.getHeader("X-Forwarded-For"),
                        req.getHeader("X-Client-DN"));
                } 
                catch (Exception e) 
                {
                    log.error(MessageFormat.format("Unable to load websocket factory: {0} ({1})", e.getClass().getName(), e.getMessage()));
                }
                
                log.trace("Failed to create websocket for {}:{} make a request at {}", req.getRemoteAddress(), req.getRemotePort(), req.getRequestPath() );
                return null;
            }
        });
    }
}
