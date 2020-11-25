package com.sercomm.openfire.plugin.component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.LocalSession;
import org.xmpp.component.AbstractComponent;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.Log;
import com.sercomm.commons.util.XStringUtil;

public class DeviceComponent extends AbstractComponent
{
    public final static String NAME = "device";
    public final static String NAMESPACE = "urn:xmpp:sercomm:demeter:1";
    public final static String DESCRIPTION = "Demeter Device Service";

    public final static String ELM_ROOT = "device";
    public final static String ELM_ARGUMENTS = "arguments";
    public final static String ATT_TYPE = "type";
    public final static String ATT_FUNCTION = "function";

    public DeviceComponent()
    {
        super();
    }
    
    @Override
    public String getDescription()
    {
        return DESCRIPTION;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    protected String[] discoInfoFeatureNamespaces() 
    {
        return new String[] { NAMESPACE };
    }
    
    @Override
    protected IQ handleIQSet(IQ requestIQ) 
    throws Exception 
    {
        JID from = requestIQ.getFrom();
        IQ resultIQ = IQ.createResultIQ(requestIQ);
        
        Element elmGateway = null;
        try
        {
            Element elmRoot = requestIQ.getChildElement();
            do
            {
                if(null == elmRoot ||
                   0 != ELM_ROOT.compareTo(elmRoot.getName()))
                {
                    break;
                }
                
                Element elmArguments = elmRoot.element(ELM_ARGUMENTS);
                if(null == elmArguments)
                {
                    break;
                }
                
                String base64String = elmArguments.getText();
                String jsonString;
                try
                {
                    jsonString = new String(Base64.getDecoder().decode(base64String));
                }
                catch(IllegalArgumentException e)
                {
                    break;
                }
                
                ArrayList<Object> arguments;
                try
                {
                    arguments = Json.mapper().readValue(
                        jsonString,
                        Json.mapper().getTypeFactory().constructCollectionType(
                            ArrayList.class,
                            Object.class));                    
                }
                catch(Throwable t1)
                {
                    // drop the stanza
                    break;
                }

                // obtain 'function'                
                Attribute attFunction = elmRoot.attribute(ATT_FUNCTION);
                if(null != attFunction)
                {
                    // protocol: v1
                    com.sercomm.openfire.plugin.websocket.v1.packet.Function function = 
                            com.sercomm.openfire.plugin.websocket.v1.packet.Function.fromString(attFunction.getText());
                    
                    switch(function)
                    {
                        case F_PING:
                            elmGateway = fPingV1(from);
                            break;
                        default:
                            elmGateway = createV1Error(
                                function,
                                com.sercomm.openfire.plugin.websocket.v1.packet.ErrorCondition.E_BAD_REQUEST, 
                                "BAD FUNCTION: " + (null != function ? function.name() : XStringUtil.BLANK));
                            break;
                    }
                }
                else
                {
                    // protocol: v0
                    com.sercomm.openfire.plugin.websocket.v0.packet.Function function = 
                            com.sercomm.openfire.plugin.websocket.v0.packet.Function.fromString((String)arguments.get(0));
                    
                    switch(function)
                    {
                        case F_PING:
                            elmGateway = fPingV0(from);
                            break;
                        default:
                            elmGateway = createV0Error(
                                com.sercomm.openfire.plugin.websocket.v0.packet.ErrorCondition.E_BAD_REQUEST, 
                                "BAD FUNCTION: " + (null != function ? function.name() : XStringUtil.BLANK));
                            break;
                    }
                }                
            }
            while(false);
        }
        catch(Throwable t)
        {
            Log.write().error(t.getMessage(), t);
        }

        if(null != elmGateway)
        {
            resultIQ.setChildElement(elmGateway);
        }

        return resultIQ;
    }
    
    private Element createV0Result(
            Object ... arguments)
    {
        Element elmRoot =
                org.dom4j.DocumentFactory.getInstance().createElement(
                    ELM_ROOT, 
                    NAMESPACE);

        // set type
        elmRoot.addAttribute(
            ATT_TYPE, 
            com.sercomm.openfire.plugin.websocket.v0.packet.Type.T_RESULT.name());
        
        // add "arguments" tag
        Element elmArguments = elmRoot.addElement(ELM_ARGUMENTS);
        elmArguments.setText(Base64.getEncoder().encodeToString(Json.build(arguments).getBytes()));

        return elmRoot;
    }
    
    private Element createV0Error(
            com.sercomm.openfire.plugin.websocket.v0.packet.ErrorCondition errorCondition, 
            String errorMessage)
    {
        Element elmRoot =
                org.dom4j.DocumentFactory.getInstance().createElement(
                    ELM_ROOT, 
                    NAMESPACE);

        // set type
        elmRoot.addAttribute(
            ATT_TYPE, 
            com.sercomm.openfire.plugin.websocket.v0.packet.Type.T_ERROR.name());
        
        // build arguments
        List<Object> arguments = new ArrayList<Object>();
        arguments.add(errorCondition);
        arguments.add(errorMessage);

        // add "arguments" tag
        Element elmArguments = elmRoot.addElement(ELM_ARGUMENTS);
        elmArguments.setText(Base64.getEncoder().encodeToString(Json.build(arguments).getBytes()));

        return elmRoot;
    }
    
    private Element fPingV0(
            JID from)
    throws Throwable
    {
        ClientSession clientSession = SessionManager.getInstance().getSession(from);
        if(null != clientSession)
        {
            if(clientSession instanceof LocalSession)
            {
                LocalSession localSession = (LocalSession) clientSession;
                localSession.setSessionData(
                    com.sercomm.openfire.plugin.websocket.v0.DeviceWebSocket.SESSION_LAST_PING, 
                    System.currentTimeMillis());
            }
        }
        
        return createV0Result();
    } 

    private Element createV1Result(
            com.sercomm.openfire.plugin.websocket.v1.packet.Function function,
            Object ... arguments)
    {
        Element elmRoot =
                org.dom4j.DocumentFactory.getInstance().createElement(
                    ELM_ROOT, 
                    NAMESPACE);

        // set attributes
        elmRoot.addAttribute(
            ATT_TYPE, 
            com.sercomm.openfire.plugin.websocket.v1.packet.Type.T_RESULT.name());
        elmRoot.addAttribute(ATT_FUNCTION, function.name());
        
        // add "arguments" tag
        Element elmArguments = elmRoot.addElement(ELM_ARGUMENTS);
        elmArguments.setText(Base64.getEncoder().encodeToString(Json.build(arguments).getBytes()));

        return elmRoot;
    }
    
    private Element createV1Error(
            com.sercomm.openfire.plugin.websocket.v1.packet.Function function,
            com.sercomm.openfire.plugin.websocket.v1.packet.ErrorCondition errorCondition, 
            String errorMessage)
    {
        Element elmRoot =
                org.dom4j.DocumentFactory.getInstance().createElement(
                    ELM_ROOT, 
                    NAMESPACE);

        // set attributes
        elmRoot.addAttribute(
            ATT_TYPE, 
            com.sercomm.openfire.plugin.websocket.v1.packet.Type.T_ERROR.name());
        elmRoot.addAttribute(ATT_FUNCTION, function.name());
        
        // build arguments
        List<Object> arguments = new ArrayList<Object>();
        arguments.add(errorCondition);
        arguments.add(errorMessage);

        // add "arguments" tag
        Element elmArguments = elmRoot.addElement(ELM_ARGUMENTS);
        elmArguments.setText(Base64.getEncoder().encodeToString(Json.build(arguments).getBytes()));

        return elmRoot;
    }
    
    private Element fPingV1(
            JID from)
    throws Throwable
    {
        ClientSession clientSession = SessionManager.getInstance().getSession(from);
        if(null != clientSession)
        {
            if(clientSession instanceof LocalSession)
            {
                LocalSession localSession = (LocalSession) clientSession;
                localSession.setSessionData(
                    com.sercomm.openfire.plugin.websocket.v1.DeviceWebSocket.SESSION_LAST_PING, 
                    System.currentTimeMillis());
            }
        }
        
        return createV1Result(com.sercomm.openfire.plugin.websocket.v1.packet.Function.F_PING);
    } 
}
