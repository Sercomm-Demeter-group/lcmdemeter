package com.sercomm.commons.util;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Json
{
    private static final ObjectMapper mapper;
    
    static {
        mapper = new ObjectMapper();
        mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    public static ObjectMapper mapper()
    {
        return mapper;
    }

    public static String build(Object object)
    {
        String output = XStringUtil.BLANK;
        try
        {
            output = mapper.writeValueAsString(object);
        }
        catch(JsonProcessingException e) {}
                
        return output;
    }

    public static JsonNode parse(String jsonString)
    throws Exception
    {
        return mapper.readTree(jsonString);
    }

    public static boolean isValid(final String jsonString)
    {
        boolean valid = false;
        try 
        {
            // try to parse the JSON string
            mapper.readTree(jsonString);

            // no exceptions occurred
            // parse successfully
            valid = true;
        } 
        catch (Throwable ignored) {}
        
        return valid;
    }
    
    public static class JavaTypeUtil
    {
        public static JavaType arrayType(
                Class<?> elementClass)
        {
            return mapper.getTypeFactory().constructArrayType(elementClass);
        }

        public static JavaType collectionType(
                Class<? extends Collection> collectionClass, 
                Class<?> elementClass)
        {
            return mapper.getTypeFactory().constructCollectionType(collectionClass, elementClass);
        }

        public static JavaType collectionLikeType(
                Class<?> collectionClass, 
                Class<?> elementClass)
        {
            return mapper.getTypeFactory().constructCollectionLikeType(collectionClass, elementClass);
        }

        public static JavaType mapType(
                Class<? extends Map> mapClass,
                Class<?> keyClass,
                Class<?> valueClass)
        {
            return mapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass);
        }

        public static JavaType mapLikeType(
                Class<?> mapClass,
                Class<?> keyClass,
                Class<?> valueClass)
        {
            return mapper.getTypeFactory().constructMapLikeType(mapClass, keyClass, valueClass);
        }
    }
}
