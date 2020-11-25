package com.sercomm.openfire.plugin;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.cluster.NodeID;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.LocalSession;
import org.jivesoftware.openfire.spi.ClientRoute;
import org.jivesoftware.openfire.spi.RoutingTableImpl;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.jivesoftware.util.cache.ClusterTask;
import org.jivesoftware.util.cache.ExternalizableUtil;
import org.xmpp.component.IQResultListener;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.util.Log;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.component.DeviceComponent;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.util.ArgumentUtil;

public class UbusManager extends ManagerBase
{
    // default "device" stanza black hole
    private IQHandler iqHandler = new IQHandler(DeviceComponent.ELM_ROOT)
    {
        private IQHandlerInfo info = new IQHandlerInfo(
            DeviceComponent.ELM_ROOT,
            DeviceComponent.NAMESPACE);
        
        @Override
        public IQ handleIQ(
                IQ packet)
            throws UnauthorizedException
        {
            // drop it, and DO NOT response anything
            return null;
        }

        @Override
        public IQHandlerInfo getInfo()
        {
            return info;
        }
    };

    private UbusManager()
    {
    }

    private static class UbusManagerContainer
    {
        private final static UbusManager instance = new UbusManager();
    }
    
    public static UbusManager getInstance()
    {
        return UbusManagerContainer.instance;
    }

    @Override
    protected void onInitialize()
    {
        XMPPServer.getInstance().getIQRouter().addHandler(this.iqHandler);
    }

    @Override
    protected void onUninitialize()
    {
        XMPPServer.getInstance().getIQRouter().removeHandler(this.iqHandler);
    }

    public Map<?,?> fire(
            String serial, 
            String mac, 
            String method, 
            String path, 
            String payloadString,
            Long timeout)
    throws DemeterException, Throwable
    {
        DeviceCache deviceCache = DeviceManager.getInstance().getDeviceCache(serial, mac);
        String modelName = deviceCache.getModelName();
        
        final String nodeName = NameRule.formatDeviceName(serial, mac);
        final JID from = new JID(DeviceComponent.NAME + "." + XMPPServer.getInstance().getServerInfo().getXMPPDomain());
        final JID to = XMPPServer.getInstance().createJID(nodeName, modelName);

        ClientSession session = SessionManager.getInstance().getSession(to);
        if(null == session)
        {
            throw new DemeterException("NO ROUTING TO TARGET DEVICE");
        }
        
        Map<?,?> dataModel = null;
        switch(deviceCache.getProtocolVersion())
        {
            case 0:
            {
                com.sercomm.openfire.plugin.websocket.v0.packet.Datagram request = 
                        com.sercomm.openfire.plugin.websocket.v0.packet.Datagram.make(
                            UUID.randomUUID().toString(), 
                            com.sercomm.openfire.plugin.websocket.v0.packet.Type.T_REQUEST);
                request.getArguments().add(com.sercomm.openfire.plugin.websocket.v0.packet.Function.F_UBUS);
                request.getArguments().add(method);
                request.getArguments().add(path);
                request.getArguments().add(payloadString);
                
                com.sercomm.openfire.plugin.websocket.v0.packet.Datagram result;
                if(session instanceof LocalSession)
                {
                    IQ requestIQ = com.sercomm.openfire.plugin.websocket.v0.packet.Datagram.convertDatagramToIQ(from, to, request);
                    IQ resultIQ = performLocalRoute(requestIQ, timeout);
                    result = com.sercomm.openfire.plugin.websocket.v0.packet.Datagram.convertIQToDatagram(resultIQ);
                }
                else
                {
                    NodeID nodeId = getClusterNodeId(to);
                    if(null == nodeId)
                    {
                        throw new DemeterException("FAILED TO OBTAIN THE CLUSTER NODE ID");
                    }
                    
                    RemoteFireTaskV0 remoteFireTask = new RemoteFireTaskV0();
                    remoteFireTask.setFrom(from.toString());
                    remoteFireTask.setTo(to.toString());
                    remoteFireTask.setRequestJSON(request.toString());
                    remoteFireTask.setTimeout(timeout);
                    
                    String resultJSON = (String) CacheFactory.doSynchronousClusterTask(
                        remoteFireTask, 
                        nodeId.toByteArray());
                    
                    //Log.write().debug("RESPONSE JSON: " + resultJSON);
                    
                    result = com.sercomm.openfire.plugin.websocket.v0.packet.Datagram.parse(resultJSON);
                }
                
                if(0 == com.sercomm.openfire.plugin.websocket.v0.packet.Type.T_ERROR.compareTo(result.getType()))
                {
                    String errorMessage = ArgumentUtil.get(result.getArguments(), 1, XStringUtil.BLANK);
                    throw new DemeterException(errorMessage);
                }
                
                dataModel = ArgumentUtil.get(result.getArguments(), 0, new HashMap<String,Object>());
                break;
            }
            case 1:
            {
                com.sercomm.openfire.plugin.websocket.v1.packet.Datagram request = 
                        com.sercomm.openfire.plugin.websocket.v1.packet.Datagram.make(
                            UUID.randomUUID().toString(), 
                            com.sercomm.openfire.plugin.websocket.v1.packet.Type.T_REQUEST,
                            com.sercomm.openfire.plugin.websocket.v1.packet.Function.F_UBUS);
                request.getArguments().add(method);
                request.getArguments().add(path);
                request.getArguments().add(payloadString);
                
                com.sercomm.openfire.plugin.websocket.v1.packet.Datagram result;
                if(session instanceof LocalSession)
                {
                    IQ requestIQ = com.sercomm.openfire.plugin.websocket.v1.packet.Datagram.convertDatagramToIQ(from, to, request);
                    IQ resultIQ = performLocalRoute(requestIQ, timeout);
                    result = com.sercomm.openfire.plugin.websocket.v1.packet.Datagram.convertIQToDatagram(resultIQ);
                }
                else
                {
                    NodeID nodeId = getClusterNodeId(to);
                    if(null == nodeId)
                    {
                        throw new DemeterException("FAILED TO OBTAIN THE CLUSTER NODE ID");
                    }
                    
                    RemoteFireTaskV1 remoteFireTask = new RemoteFireTaskV1();
                    remoteFireTask.setFrom(from.toString());
                    remoteFireTask.setTo(to.toString());
                    remoteFireTask.setRequestJSON(request.toString());
                    remoteFireTask.setTimeout(timeout);
                    
                    String resultJSON = (String) CacheFactory.doSynchronousClusterTask(
                        remoteFireTask, 
                        nodeId.toByteArray());
                    
                    //Log.write().debug("RESPONSE JSON: " + resultJSON);
                    
                    result = com.sercomm.openfire.plugin.websocket.v1.packet.Datagram.parse(resultJSON);
                }
                
                if(0 == com.sercomm.openfire.plugin.websocket.v1.packet.Type.T_ERROR.compareTo(result.getType()))
                {
                    String errorMessage = ArgumentUtil.get(result.getArguments(), 1, XStringUtil.BLANK);
                    throw new DemeterException(errorMessage);
                }
                
                dataModel = ArgumentUtil.get(result.getArguments(), 0, new HashMap<String,Object>());
                break;
            }
        }
        
        return dataModel;
    }
    
    private static IQ performLocalRoute(IQ requestIQ, long timeout)
    throws InterruptedException
    {
        final IQRouter router = XMPPServer.getInstance().getIQRouter();        
        final IQSyncResultListener listener = new IQSyncResultListener(requestIQ, timeout);
        
        router.addIQResultListener(
            requestIQ.getID(), 
            listener, 
            timeout);

        // route the IQ stanza
        router.route(requestIQ);
        
        synchronized(listener)
        {
            listener.wait();
        }        

        return listener.getResultIQ();
    }

    @SuppressWarnings("all")
    private static NodeID getClusterNodeId(JID address)
    {
        NodeID nodeID = null;
        Cache<String, ClientRoute> usersCache = CacheFactory.createCache(RoutingTableImpl.C2S_CACHE_NAME);
        Cache<String, ClientRoute> anonymousUsersCache = CacheFactory.createCache(RoutingTableImpl.ANONYMOUS_C2S_CACHE_NAME);
        
        ClientRoute route = usersCache.get(address.toString());
        if (route == null) 
        {
            route = anonymousUsersCache.get(address.toString());
        }
        
        if(route != null)
        {
            nodeID = route.getNodeID();
        }
        
        return nodeID;
    }

    public static class RemoteFireTaskV0 implements ClusterTask<String>
    {
        private String from;
        private String to;
        private String requestJSON;
        private String resultJSON;
        private Long timeout;
        
        public RemoteFireTaskV0()
        {
        }
        
        public String getFrom()
        {
            return this.from;
        }
        
        public void setFrom(String from)
        {
            this.from = from;
        }
        
        public String getTo()
        {
            return this.to;
        }
        
        public void setTo(String to)
        {
            this.to = to;
        }
        
        public String getRequestJSON()
        {
            return requestJSON;
        }

        public void setRequestJSON(String requestJSON)
        {
            this.requestJSON = requestJSON;
        }

        public String getResultJSON()
        {
            return resultJSON;
        }

        public void setResultJSON(
                String resultJSON)
        {
            this.resultJSON = resultJSON;
        }
        
        public Long getTimeout()
        {
            return this.timeout;
        }
        
        public void setTimeout(Long timeout)
        {
            this.timeout = timeout;
        }
        
        @Override
        public void run()
        {
            Log.write().info("({},{});", this.to, this.requestJSON);
            
            com.sercomm.openfire.plugin.websocket.v0.packet.Datagram result = null;
            try
            {
                com.sercomm.openfire.plugin.websocket.v0.packet.Datagram request = 
                        com.sercomm.openfire.plugin.websocket.v0.packet.Datagram.parse(this.requestJSON);
                IQ requestIQ = com.sercomm.openfire.plugin.websocket.v0.packet.Datagram.convertDatagramToIQ(
                    new JID(this.from), 
                    new JID(this.to), 
                    request);
                
                IQ resultIQ = performLocalRoute(requestIQ, this.timeout);
                
                result = com.sercomm.openfire.plugin.websocket.v0.packet.Datagram.convertIQToDatagram(resultIQ);
            }
            catch(Throwable t)
            {
                Log.write().error(t.getMessage(), t);
            }

            this.setResultJSON(result.toString());
        }

        @Override
        public void writeExternal(
                ObjectOutput out)
        throws IOException
        {
            ExternalizableUtil.getInstance().writeSafeUTF(out, this.from);
            ExternalizableUtil.getInstance().writeSafeUTF(out, this.to);
            ExternalizableUtil.getInstance().writeSafeUTF(out, this.requestJSON);
            ExternalizableUtil.getInstance().writeSafeUTF(out, this.resultJSON);           
            ExternalizableUtil.getInstance().writeLong(out, this.timeout);
        }

        @Override
        public void readExternal(
                ObjectInput in)
        throws IOException, ClassNotFoundException
        {
            this.from = ExternalizableUtil.getInstance().readSafeUTF(in);
            this.to = ExternalizableUtil.getInstance().readSafeUTF(in);
            this.requestJSON = ExternalizableUtil.getInstance().readSafeUTF(in);
            this.resultJSON = ExternalizableUtil.getInstance().readSafeUTF(in);
            this.timeout = ExternalizableUtil.getInstance().readLong(in);
        }

        @Override
        public String getResult()
        {
            return this.resultJSON;
        }
    }

    public static class RemoteFireTaskV1 implements ClusterTask<String>
    {
        private String from;
        private String to;
        private String requestJSON;
        private String resultJSON;
        private Long timeout;
        
        public RemoteFireTaskV1()
        {
        }
        
        public String getFrom()
        {
            return this.from;
        }
        
        public void setFrom(String from)
        {
            this.from = from;
        }
        
        public String getTo()
        {
            return this.to;
        }
        
        public void setTo(String to)
        {
            this.to = to;
        }
        
        public String getRequestJSON()
        {
            return requestJSON;
        }

        public void setRequestJSON(String requestJSON)
        {
            this.requestJSON = requestJSON;
        }

        public String getResultJSON()
        {
            return resultJSON;
        }

        public void setResultJSON(
                String resultJSON)
        {
            this.resultJSON = resultJSON;
        }
        
        public Long getTimeout()
        {
            return this.timeout;
        }
        
        public void setTimeout(Long timeout)
        {
            this.timeout = timeout;
        }
        
        @Override
        public void run()
        {
            Log.write().info("({},{});", this.to, this.requestJSON);
            
            com.sercomm.openfire.plugin.websocket.v1.packet.Datagram result = null;
            try
            {
                com.sercomm.openfire.plugin.websocket.v1.packet.Datagram request = 
                        com.sercomm.openfire.plugin.websocket.v1.packet.Datagram.parse(this.requestJSON);
                IQ requestIQ = com.sercomm.openfire.plugin.websocket.v1.packet.Datagram.convertDatagramToIQ(
                    new JID(this.from), 
                    new JID(this.to), 
                    request);
                
                IQ resultIQ = performLocalRoute(requestIQ, this.timeout);
                
                result = com.sercomm.openfire.plugin.websocket.v1.packet.Datagram.convertIQToDatagram(resultIQ);
            }
            catch(Throwable t)
            {
                Log.write().error(t.getMessage(), t);
            }

            this.setResultJSON(result.toString());
        }

        @Override
        public void writeExternal(
                ObjectOutput out)
        throws IOException
        {
            ExternalizableUtil.getInstance().writeSafeUTF(out, this.from);
            ExternalizableUtil.getInstance().writeSafeUTF(out, this.to);
            ExternalizableUtil.getInstance().writeSafeUTF(out, this.requestJSON);
            ExternalizableUtil.getInstance().writeSafeUTF(out, this.resultJSON);           
            ExternalizableUtil.getInstance().writeLong(out, this.timeout);
        }

        @Override
        public void readExternal(
                ObjectInput in)
        throws IOException, ClassNotFoundException
        {
            this.from = ExternalizableUtil.getInstance().readSafeUTF(in);
            this.to = ExternalizableUtil.getInstance().readSafeUTF(in);
            this.requestJSON = ExternalizableUtil.getInstance().readSafeUTF(in);
            this.resultJSON = ExternalizableUtil.getInstance().readSafeUTF(in);
            this.timeout = ExternalizableUtil.getInstance().readLong(in);
        }

        @Override
        public String getResult()
        {
            return this.resultJSON;
        }
    }

    public static class IQSyncResultListener 
    implements IQResultListener
    {
        private IQ requestIQ;
        private IQ resultIQ;
        private Long timeout;

        public IQSyncResultListener(IQ requestIQ, long timeout)
        {
            this.requestIQ = requestIQ;
            this.timeout = timeout;
        }
        
        public IQ getResultIQ()
        {
            return this.resultIQ;
        }
        
        public long getTimeout()
        {
            return this.timeout;
        }
        
        @Override
        public void answerTimeout(String id)
        {
            try
            {
                this.resultIQ = IQ.createResultIQ(this.requestIQ);
                this.resultIQ.setError(new PacketError(
                    PacketError.Condition.remote_server_timeout, 
                    PacketError.Type.cancel,
                    MessageFormat.format("TIMEOUT:{0,number,#}", this.timeout)));
            }
            finally
            {
                synchronized(this)
                {
                    this.notifyAll();
                }
            }            
        }

        @Override
        public void receivedAnswer(IQ resultIQ)
        {
            try
            {
                this.resultIQ = resultIQ;            
            }
            finally
            {
                synchronized(this)
                {
                    this.notifyAll();
                }
            }
        }
    }
}
