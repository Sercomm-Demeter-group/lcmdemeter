package com.sercomm.commons.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class Yaml
{
    private static final ObjectMapper mapper;
    static {
        mapper = new ObjectMapper(new YAMLFactory());
    }
    
    public static class Node
    {
        private JsonNode jsonNode;
        
        private Node(JsonNode jsonNode)
        {
            this.jsonNode = jsonNode;
        }
        
        public Node findPath(String fieldName)
        {
            return new Node(this.jsonNode.findPath(fieldName));
        }
        
        public String asText(String defaultValue)
        {
            return this.jsonNode.asText(defaultValue);
        }
        
        public String asText()
        {
            return this.jsonNode.asText();
        }
    }
    
    public static Node load(InputStream inputStream)
    throws IOException
    {
        return new Node(mapper.readTree(inputStream));
    }

    public static Node load(String filePath)
    throws IOException
    {
        try(InputStream inputStream = new FileInputStream(filePath))
        {
            return load(inputStream);
        }
    }
}
