package com.sercomm.commons.umei;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Datagram
{
    private interface JsonViewer {};
    
    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        // allow datagram contains JSON string
        mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.setSerializationInclusion(Include.NON_NULL);
    }
    
    @JsonView(JsonViewer.class)
    private HeaderPayload header;
    @JsonView(JsonViewer.class)
    private BodyPayload body;

    public Datagram()
    {
    }

    @JsonIgnore
    public HeaderPayload getHeaderPayload()
    {
        return this.header;
    }
    
    @JsonIgnore
    public BodyPayload getBodyPayload()
    {
        return this.body;
    }

    @JsonIgnore
    public Datagram withHeaderPayload(HeaderPayload headerPayload)
    {
        this.header = headerPayload;
        return this;
    }

    @JsonIgnore
    public Datagram withBodyPayload(BodyPayload bodyPayload)
    {
        this.body = bodyPayload;
        return this;
    }

    @JsonIgnore
    public boolean isRequest()
    {
        boolean ok = false;
        
        String value = this.header.getHeaders().get(HeaderField.HEADER_MESSAGE_TYPE);
        if(StringUtils.isNotBlank(value))
        {
            if(0 == value.compareTo(HeaderPayload.TYPE_REQUEST))
            {
                ok = true;
            }
        }
        
        return ok;
    }
    
    @JsonIgnore
    public boolean isResult()
    {
        boolean ok = false;
        
        String value = this.header.getHeaders().get(HeaderField.HEADER_MESSAGE_TYPE);
        if(StringUtils.isNotBlank(value))
        {
            if(0 == value.compareTo(HeaderPayload.TYPE_RESULT))
            {
                ok = true;
            }
        }
        
        return ok;
    }
    
    @JsonIgnore
    @Override
    public String toString()
    {
        String value;
        try
        {
            value = mapper.writeValueAsString(this);
        }
        catch(JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
        
        return value;
    }
    
    public static Datagram fromString(String jsonString)
    {
        Datagram object = null;
        try
        {
            object = mapper.readValue(jsonString, Datagram.class);
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        
        return object;
    }
}
