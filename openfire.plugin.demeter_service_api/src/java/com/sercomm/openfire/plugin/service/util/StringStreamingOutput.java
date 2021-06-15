package com.sercomm.openfire.plugin.service.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

public class StringStreamingOutput implements StreamingOutput 
{
    private static final int BUFFER_LENGTH = 256;

    private String value;
    public StringStreamingOutput(String value)
    {
        this.value = value;
    }
    
    @Override
    public void write(OutputStream output)
    throws IOException, WebApplicationException 
    {
        try(InputStream is = 
                new ByteArrayInputStream(this.value.getBytes(StandardCharsets.UTF_8)))
        {
            byte[] buffer = new byte[BUFFER_LENGTH];
            int length = 0;
            
            while((length = is.read(buffer, 0, BUFFER_LENGTH)) != -1) 
            {
                output.write(buffer, 0, length);
            }
            output.flush();
        } 
        catch(Throwable t) 
        {
            throw new WebApplicationException(t.getMessage(), t);
        }
    }
}
