package com.sercomm.openfire.plugin.websocket.v0;

import java.net.InetSocketAddress;

import org.jivesoftware.openfire.*;
import org.jivesoftware.openfire.PacketDeliverer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.net.VirtualConnection;
import org.jivesoftware.openfire.nio.OfflinePacketDeliverer;
import org.jivesoftware.openfire.session.LocalClientSession;
import org.jivesoftware.openfire.session.LocalSession;
import org.jivesoftware.openfire.spi.ConnectionConfiguration;
import org.jivesoftware.openfire.spi.ConnectionManagerImpl;
import org.jivesoftware.openfire.spi.ConnectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;

import com.sercomm.commons.util.Json;
import com.sercomm.openfire.plugin.websocket.v0.packet.Datagram;

public class WebSocketConnection extends VirtualConnection
{
    private static final Logger log = LoggerFactory.getLogger(WebSocketConnection.class);

    private InetSocketAddress remotePeer;
    private DeviceWebSocket socket;
    private PacketDeliverer backupDeliverer;
    private ConnectionConfiguration configuration;
    private ConnectionType connectionType;

    public WebSocketConnection(DeviceWebSocket socket, InetSocketAddress remotePeer) 
    {
        this.socket = socket;
        this.remotePeer = remotePeer;
        this.connectionType = ConnectionType.SOCKET_C2S;
    }

    @Override
    public void closeVirtualConnection()
    {
        socket.close();
    }

    @Override
    public byte[] getAddress() 
    {
        return remotePeer.getAddress().getAddress();
    }

    @Override
    public String getHostAddress() 
    {
        return remotePeer.getAddress().getHostAddress();
    }

    @Override
    public String getHostName()
    {
        return remotePeer.getHostName();
    }

    @Override
    public void systemShutdown() 
    {
        close();
    }

    @Override
    public void deliver(Packet packet) throws UnauthorizedException
    {
        // only IQ stanza which contains "gateway" element is allowed 
        // to deliver to device
        if(false == (packet instanceof IQ))
        {
            log.error("ONLY IQ STANZA IS ALLOWED FOR THIS SESSION: " + packet.toXML());
            return;
        }
        
        // convert stanza to datagram
        IQ stanza = (IQ) packet;
        
        Datagram datagram;
        try
        {
            datagram = Datagram.convertIQToDatagram(stanza);
        }
        catch(Throwable t)
        {
            log.error(t.getMessage());
            return;
        }
        
        deliverRawText(Json.build(datagram));
    }

    @Override
    public void deliverRawText(String text)
    {
        socket.deliver(text);
    }

    @Override
    public boolean validate() 
    {
        return socket.isOpen();
    }

    @Override
    public boolean isSecure() 
    {
        return socket.isSecure();
    }

    @Override
    public PacketDeliverer getPacketDeliverer() 
    {
        if (backupDeliverer == null) 
        {
            backupDeliverer = new OfflinePacketDeliverer();
        }
        
        return backupDeliverer;
    }

    @Override
    public ConnectionConfiguration getConfiguration() 
    {
        if(configuration == null) 
        {
            final ConnectionManagerImpl connectionManager = ((ConnectionManagerImpl) XMPPServer.getInstance().getConnectionManager());
            configuration = connectionManager.getListener( connectionType, true ).generateConnectionConfiguration();
        }
        
        return configuration;
    }

    @Override
    public boolean isCompressed() 
    {
        return DeviceWebSocket.isCompressionEnabled();
    }

    @Override
    public void reinit(LocalSession session) 
    {
        this.socket.setLocalClientSession((LocalClientSession)session);
        super.reinit(session);
    }
}
