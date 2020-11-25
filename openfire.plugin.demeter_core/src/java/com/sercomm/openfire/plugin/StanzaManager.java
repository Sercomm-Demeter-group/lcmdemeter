package com.sercomm.openfire.plugin;

import com.sercomm.common.util.ManagerBase;

public class StanzaManager extends ManagerBase
{
    private static class StanzaManagerContainer
    {
        private final static StanzaManager instance = new StanzaManager();
    }
    /*
    private final PacketInterceptor interceptor = new PacketInterceptor()
    {
        @Override
        public void interceptPacket(
                Packet packet,
                Session session,
                boolean incoming,
                boolean processed)
        throws PacketRejectedException
        {
            // only intercept stanza which has not been processed
            if(true == processed)
            {
                return;
            }

            // only intercept stanza which is incoming
            if(false == incoming)
            {
                return;
            }
            
            // only intercept stanza which is from local session
            if(false == (session instanceof LocalSession))
            {
                return;
            }

            Element root = packet.getElement();
            if(null == root)
            {
                return;
            }
            
            Element element;
            // check if the stanza should be processed by UbusManager
            element = root.element(DeviceComponent.ELM_ROOT);
            if(null == element)
            {
                return;
            }

            IQ stanza = (IQ) packet;
            
            // definition: only Type.set should be processed by UbusManager
            if(stanza.getType() != Type.set)
            {
                return;
            }
            
            final JID from = stanza.getFrom();
            final JID to = stanza.getTo();
            
            // forward incoming packets to defined destinations
            UbusManager.getInstance().handleCustomIQ(session, from, to, stanza);
            // then re-direct the packet to system black hole
            packet.setTo(XMPPServer.getInstance().getServerInfo().getXMPPDomain());
        }
    };
    */
    
    private StanzaManager()
    {
    }

    public static StanzaManager getInstance()
    {
        return StanzaManagerContainer.instance;
    }
    
    @Override
    protected void onInitialize()
    {
        //InterceptorManager.getInstance().addInterceptor(this.interceptor);
    }

    @Override
    protected void onUninitialize()
    {
        //InterceptorManager.getInstance().removeInterceptor(this.interceptor);
    }
}
