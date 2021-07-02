package com.sercomm.openfire.plugin.websocket.v1.packet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.component.DeviceComponent;
import com.sercomm.openfire.plugin.exception.DemeterException;

public class Datagram implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String id;
    private Type type;
    private Function function;
    private final List<Object> arguments = new ArrayList<Object>();
    
    public Datagram()
    {
    }
    
    public String getId()
    {
        return this.id;
    }
    
    public void setId(String id)
    {
        this.id = id;
    }
    
    public Type getType()
    {
        return this.type;
    }
    
    public void setType(Type type)
    {
        this.type = type;
    }

    public Function getFunction()
    {
        return this.function;
    }
    
    public void setFunction(Function function)
    {
        this.function = function;
    }
    
    public List<Object> getArguments()
    {
        return this.arguments;
    }

    @Override
    public String toString()
    {
        return Json.build(this);
    }
    
    public static Datagram parse(String text)
    throws Throwable
    {
        return Json.mapper().readValue(
            text, 
            Datagram.class);
    }
    
    public static Datagram make(
            String id,
            Type type,
            Function function)
    {
        Datagram packet = new Datagram();
        packet.id = id;
        packet.type = type;
        packet.function = function;
        
        return packet;
    }
    
    public static IQ convertDatagramToIQ(JID from, JID to, Datagram datagram)
    {
        final IQ requestIQ = new IQ();
        requestIQ.setID(datagram.getId());
        requestIQ.setType(org.xmpp.packet.IQ.Type.set);
        requestIQ.setFrom(from);
        requestIQ.setTo(to);
        
        Element elmRoot = requestIQ.setChildElement(
            DeviceComponent.ELM_ROOT, 
            DeviceComponent.NAMESPACE);
        elmRoot.addAttribute(DeviceComponent.ATT_TYPE, datagram.getType().name());
        elmRoot.addAttribute(DeviceComponent.ATT_FUNCTION, datagram.getFunction().name());
        
        Element elmArguments = elmRoot.addElement(DeviceComponent.ELM_ARGUMENTS);
        elmArguments.setText(Base64.getEncoder().encodeToString(Json.build(datagram.getArguments()).getBytes()));

        return requestIQ;
    }
    
    public static Datagram convertIQToDatagram(IQ stanza)
    throws DemeterException
    {
        Element element = stanza.getChildElement();
        if(null == element ||
           0 != DeviceComponent.ELM_ROOT.compareTo(element.getName()))
        {
            throw new DemeterException("NO ROOT ELEMENT AVAILABLE: " + Json.xmlToJson(stanza.toXML()));
        }

        String idString = stanza.getID();
        String typeString = element.attributeValue(DeviceComponent.ATT_TYPE);
        String functionString = element.attributeValue(DeviceComponent.ATT_FUNCTION);
        
        if(XStringUtil.isBlank(idString) ||
           XStringUtil.isBlank(typeString) ||
           XStringUtil.isBlank(functionString))
        {
            throw new DemeterException("ID/TYPE/FUNCTION IS BLANK: " + Json.xmlToJson(stanza.toXML()));
        }

        Datagram datagram = Datagram.make(
            idString, 
            Type.fromString(typeString),
            Function.fromString(functionString));
        try
        {
            Element elmArguments = element.element(DeviceComponent.ELM_ARGUMENTS);
            String resultJSON = new String(Base64.getDecoder().decode(elmArguments.getText()));
            
            List<Object> arguments = Json.mapper().readValue(
                resultJSON,
                Json.mapper().getTypeFactory().constructCollectionType(
                    ArrayList.class,
                    Object.class));  
            
            for(Object argument : arguments)
            {
                datagram.getArguments().add(argument);
            }
        }
        catch(Throwable t)
        {
            throw new DemeterException("INVALID ARGUMENTS FORMAT: " + Json.xmlToJson(stanza.toXML()));
        }

        return datagram;
    }
}
