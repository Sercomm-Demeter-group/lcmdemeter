package com.sercomm.commons.umei;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BodyPayload
{
    private interface JsonViewer {};

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.setSerializationInclusion(Include.NON_NULL);
    }

    @JsonView(JsonViewer.class)
    private Object desire;
    @JsonView(JsonViewer.class)
    private Meta meta;
    @JsonView(JsonViewer.class)
    private Object data;
    @JsonView(JsonViewer.class)
    private List<UMEiError> errors;
    
    public BodyPayload()
    {
    }

    // from a JSON string
    public static BodyPayload from(String jsonString)
    throws Exception
    {
        BodyPayload bodyPayload = mapper.readValue(jsonString, BodyPayload.class);
        return bodyPayload;
    }

    @JsonIgnore
    public Meta getMeta()
    {
        return meta;
    }

    @JsonIgnore
    public <T> T getDesire(Class<T> clazz)
    {
        return mapper.convertValue(this.desire, clazz);
    }

    @JsonIgnore
    public <T> T getDesire(JavaType javaType)
    {
        return mapper.convertValue(this.desire, javaType);
    }

    @JsonIgnore
    public <T> T getData(Class<T> clazz)
    {
        return mapper.convertValue(this.data, clazz);
    }

    @JsonIgnore
    public <T> T getData(JavaType javaType)
    {
        return mapper.convertValue(this.data, javaType);
    }

    @JsonIgnore
    public List<UMEiError> getErrors()
    {
        return this.errors;
    }
    
    @JsonIgnore
    public BodyPayload withDesire(Object desire)
    {
        this.desire = desire;
        return this;
    }
    
    @JsonIgnore
    public BodyPayload withMeta(Meta meta)
    {
        this.meta = meta;
        return this;
    }

    @JsonIgnore
    public BodyPayload withData(Object data)
    {
        this.data = data;
        return this;
    }

    @JsonIgnore
    public BodyPayload withError(int code, String detail)
    {
        if(null == this.errors)
        {
            this.errors = new ArrayList<UMEiError>();
        }
        
        UMEiError error = new UMEiError();
        error.setCode(code);
        error.setDetail(StringUtils.isBlank(detail) ? "" : detail);

        this.errors.add(error);
        
        return this;
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
}
