package com.sercomm.commons.umei;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HeaderPayload
{
    private interface JsonViewer {};

    protected static final String TYPE_REQUEST  = "Request";
    protected static final String TYPE_RESULT   = "Result";

    private static final long DEFAULT_EXPIRATION_INTERVAL = 15 * 1000L;
    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        // allow datagram contains JSON string
        mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        mapper.setSerializationInclusion(Include.NON_NULL);
    }

    @JsonView(JsonViewer.class)
    private Map<String, String> headers;
    @JsonView(JsonViewer.class)
    private String path;
    @JsonView(JsonViewer.class)
    private String method;

    public HeaderPayload()
    {
    }
    
    @JsonIgnore
    public Map<String, String> getHeaders()
    {
        return this.headers;
    }
    
    @JsonIgnore
    public String getRequestId()
    {
        return this.headers.get(HeaderField.HEADER_REQUEST_ID);
    }
    
    @JsonIgnore
    public String getOriginatorId()
    {
        return this.headers.get(HeaderField.HEADER_ORIGINATOR_ID);
    }
    
    @JsonIgnore
    public String getReceiverId()
    {
        return this.headers.get(HeaderField.HEADER_RECEIVER_ID);
    }
    
    @JsonIgnore
    public long getCreationTime()
    {
        long creationTime = 0L;
        
        String rawData = this.headers.get(HeaderField.HEADER_CREATION_TIME);
        if(null != rawData)
        {
            creationTime = Long.parseLong(rawData);
        }
        
        return creationTime;
    }
    
    @JsonIgnore
    public long getExpirationTime()
    {
        long expirationTime = 0L;
        
        String value = this.headers.get(HeaderField.HEADER_EXPIRATION_TIME);
        if(StringUtils.isNotBlank(value))
        {
            expirationTime = Long.parseLong(value);
        }
        
        return expirationTime;
    }
    
    @JsonIgnore
    public boolean isRequest()
    {
        boolean ok = false;
        
        String value = this.headers.get(HeaderField.HEADER_MESSAGE_TYPE);
        if(StringUtils.isNotBlank(value))
        {
            if(0 == value.compareTo(TYPE_REQUEST))
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
        
        String value = this.headers.get(HeaderField.HEADER_MESSAGE_TYPE);
        if(StringUtils.isNotBlank(value))
        {
            if(0 == value.compareTo(TYPE_RESULT))
            {
                ok = true;
            }
        }
        
        return ok;
    }
    
    @JsonIgnore
    public String getPath()
    {
        return this.path;
    }
    
    @JsonIgnore
    public String getMethod()
    {
        return this.method;
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
    
    public static class RequestBuilder
    {
        private Map<String, String> headers;
        private String path;
        private String method;
        private long expirationInterval = DEFAULT_EXPIRATION_INTERVAL;

        public RequestBuilder()
        {
            this.headers = new HashMap<String, String>();
            this.headers.put(HeaderField.HEADER_MESSAGE_TYPE, HeaderPayload.TYPE_REQUEST);
        }
        
        public HeaderPayload build()
        {
            long creationTime = System.currentTimeMillis();
            long expirationTime = 0L;
            if(0L != this.expirationInterval)
            {
                expirationTime = creationTime + this.expirationInterval;
            }
            
            this.headers.put(HeaderField.HEADER_CREATION_TIME, Long.toString(creationTime));
            this.headers.put(HeaderField.HEADER_EXPIRATION_TIME, Long.toString(expirationTime));

            HeaderPayload headerPayload = new HeaderPayload();
            headerPayload.headers = this.headers;
            headerPayload.path = this.path;
            if(StringUtils.isBlank(this.path))
            {
                throw new RuntimeException("PATH CANNOT BE BLANK");
            }
            
            if(!this.path.startsWith("/"))
            {
                throw new RuntimeException("PATH MUST START WITH A SLASH, GOT \"" + this.path + "\"");
            }
            
            headerPayload.method = this.method;

            RequestBuilder.assertObject(headerPayload);
            
            return headerPayload;
        }

        public static HeaderPayload build(String jsonString)
        throws Exception
        {
            HeaderPayload headerPayload = mapper.readValue(jsonString, HeaderPayload.class);
            RequestBuilder.assertObject(headerPayload);
            return headerPayload;
        }
        
        public RequestBuilder requestId(String requestId)
        {
            this.headers.put(HeaderField.HEADER_REQUEST_ID, requestId);
            return this;
        }

        public RequestBuilder originatorId(String originatorId)
        {
            this.headers.put(HeaderField.HEADER_ORIGINATOR_ID, originatorId);
            return this;
        }
        
        public RequestBuilder expirationInterval(long expirationInterval)
        {
            this.expirationInterval = expirationInterval;
            return this;
        }
        
        public RequestBuilder path(String path)
        {
            this.path = path;
            return this;
        }
        
        public RequestBuilder method(String method)
        {
            this.method = method;
            return this;
        }
        
        private static void assertObject(HeaderPayload headerPayload)
        {
            String value;

            value = headerPayload.headers.get(HeaderField.HEADER_REQUEST_ID);
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException(HeaderField.HEADER_REQUEST_ID + " CANNOT BE BLANK");
            }

            value = headerPayload.headers.get(HeaderField.HEADER_ORIGINATOR_ID);
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException(HeaderField.HEADER_ORIGINATOR_ID + " CANNOT BE BLANK");
            }

            value = headerPayload.headers.get(HeaderField.HEADER_CREATION_TIME);
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException(
                    String.format("'%s' CANNOT BE BLANK", HeaderField.HEADER_CREATION_TIME));
            }
            // try to parse for checking if it is a valid long value
            Long.parseLong(value);

            value = headerPayload.headers.get(HeaderField.HEADER_EXPIRATION_TIME);
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException(
                    String.format("'%s' CANNOT BE BLANK", HeaderField.HEADER_EXPIRATION_TIME));
            }
            // try to parse for checking if it is a valid long value
            Long.parseLong(value);
            
            value = headerPayload.headers.get(HeaderField.HEADER_MESSAGE_TYPE);
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException(
                    String.format("'%s' CANNOT BE BLANK", HeaderField.HEADER_MESSAGE_TYPE));
            }

            if(0 != value.compareTo(HeaderPayload.TYPE_REQUEST))
            {
                throw new RuntimeException(
                    String.format("INVALID '%s': %s", HeaderField.HEADER_MESSAGE_TYPE, value));
            }
            
            value = headerPayload.path;
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException("'path' CANNOT BE BLANK");
            }
            
            value = headerPayload.method;
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException("'method' CANNOT BE BLANK");
            }
        }
    }
    
    public static class ResultBuilder
    {
        private Map<String, String> headers;
        private String path;
        private String method;
        
        public ResultBuilder()
        {
            this.headers = new HashMap<String, String>();
            this.headers.put(HeaderField.HEADER_MESSAGE_TYPE, HeaderPayload.TYPE_REQUEST);
        }
        
        public HeaderPayload build()
        {
            HeaderPayload headerPayload = new HeaderPayload();
            headerPayload.headers = this.headers;
            headerPayload.path = this.path;
            headerPayload.method = this.method;

            ResultBuilder.assertObject(headerPayload);

            return headerPayload;
        }

        public static HeaderPayload build(String jsonString)
        throws Exception
        {
            HeaderPayload headerPayload = mapper.readValue(jsonString, HeaderPayload.class);
            ResultBuilder.assertObject(headerPayload);
            return headerPayload;
        }

        public ResultBuilder requestId(String requestId)
        {
            this.headers.put(HeaderField.HEADER_REQUEST_ID, requestId);
            return this;
        }

        public ResultBuilder originatorId(String originatorId)
        {
            this.headers.put(HeaderField.HEADER_ORIGINATOR_ID, originatorId);
            return this;
        }

        public ResultBuilder receiverId(String receiverId)
        {
            this.headers.put(HeaderField.HEADER_RECEIVER_ID, receiverId);
            return this;
        }
        
        public ResultBuilder creationTime(long creationTime)
        {
            this.headers.put(HeaderField.HEADER_CREATION_TIME, Long.toString(creationTime));
            return this;
        }
        
        public ResultBuilder expirationTime(long expirationTime)
        {
            this.headers.put(HeaderField.HEADER_EXPIRATION_TIME, Long.toString(expirationTime));
            return this;
        }
        
        public ResultBuilder path(String path)
        {
            this.path = path;
            return this;
        }
        
        public ResultBuilder method(String method)
        {
            this.method = method;
            return this;
        }

        private static void assertObject(HeaderPayload headerPayload)
        {
            String value;

            value = headerPayload.headers.get(HeaderField.HEADER_REQUEST_ID);
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException(HeaderField.HEADER_REQUEST_ID + " CANNOT BE BLANK");
            }

            value = headerPayload.headers.get(HeaderField.HEADER_ORIGINATOR_ID);
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException(HeaderField.HEADER_ORIGINATOR_ID + " CANNOT BE BLANK");
            }

            value = headerPayload.headers.get(HeaderField.HEADER_RECEIVER_ID);
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException(HeaderField.HEADER_RECEIVER_ID + " CANNOT BE BLANK");
            }
            
            value = headerPayload.headers.get(HeaderField.HEADER_CREATION_TIME);
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException(
                    String.format("'%s' CANNOT BE BLANK", HeaderField.HEADER_CREATION_TIME));
            }
            // try to parse for checking if it is a valid long value
            Long.parseLong(value);

            value = headerPayload.headers.get(HeaderField.HEADER_EXPIRATION_TIME);
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException(
                    String.format("'%s' CANNOT BE BLANK", HeaderField.HEADER_EXPIRATION_TIME));
            }
            // try to parse for checking if it is a valid long value
            Long.parseLong(value);
            
            value = headerPayload.headers.get(HeaderField.HEADER_MESSAGE_TYPE);
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException(
                    String.format("'%s' CANNOT BE BLANK", HeaderField.HEADER_MESSAGE_TYPE));
            }

            if(0 != value.compareTo(HeaderPayload.TYPE_RESULT))
            {
                throw new RuntimeException(
                    String.format("INVALID '%s': %s", HeaderField.HEADER_MESSAGE_TYPE, value));
            }
            
            value = headerPayload.path;
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException("'path' CANNOT BE BLANK");
            }
            
            value = headerPayload.method;
            if(StringUtils.isBlank(value))
            {
                throw new RuntimeException("'method' CANNOT BE BLANK");
            }
        }
    }    
}
