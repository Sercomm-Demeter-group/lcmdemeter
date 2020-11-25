package com.sercomm.commons.umei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.bigtesting.routd.Route;
import org.bigtesting.routd.Router;
import org.bigtesting.routd.TreeRouter;

import com.sercomm.commons.umei.Datagram;
import com.sercomm.commons.umei.annotation.method.Data;
import com.sercomm.commons.umei.annotation.method.Desire;
import com.sercomm.commons.umei.annotation.method.Issuer;
import com.sercomm.commons.umei.annotation.method.Origin;
import com.sercomm.commons.umei.annotation.method.PathParam;
import com.sercomm.commons.umei.annotation.method.QueryParam;
import com.sercomm.commons.umei.annotation.method.ReceivedTime;
import com.sercomm.commons.umei.annotation.path.DatagramPath;
import com.sercomm.commons.umei.annotation.verb.DELETE;
import com.sercomm.commons.umei.annotation.verb.GET;
import com.sercomm.commons.umei.annotation.verb.PATCH;
import com.sercomm.commons.umei.annotation.verb.POST;
import com.sercomm.commons.umei.annotation.verb.PUT;

public class DatagramRouter
{    
    private Router router = new TreeRouter();
    private final Map<String, java.lang.reflect.Method> reflectMethods = new ConcurrentHashMap<>();

    public DatagramRouter()
    {
    }
    
    public void register(Class<?> clazz)
    {
        DatagramPath prefixPathPattern = clazz.getAnnotation(DatagramPath.class);
        if(null == prefixPathPattern)
        {
            throw new RuntimeException(String.format("MISSING '%s' ANNOTATION DELCARATION IN CLASS '%s'", DatagramPath.class.getSimpleName(), clazz.getName()));
        }
        
        for(java.lang.reflect.Method reflectMethod : clazz.getMethods())
        {
            // if the "DatagramPath" declaration can be found
            if(reflectMethod.isAnnotationPresent(DatagramPath.class))
            {
                DatagramPath postfixPathPattern = reflectMethod.getAnnotation(DatagramPath.class);
                final String pathPattern = prefixPathPattern.value() + postfixPathPattern.value();
                if(false == pathPattern.startsWith("/"))
                {
                    throw new RuntimeException("PATH MUST START WITH A SLASH, GOT \"" + pathPattern + "\"");
                }

                String requestMethod = null;
                if(reflectMethod.isAnnotationPresent(GET.class))
                {
                    GET verb = reflectMethod.getAnnotation(GET.class);
                    requestMethod = verb.value();
                }
                else if(reflectMethod.isAnnotationPresent(POST.class))
                {
                    POST verb = reflectMethod.getAnnotation(POST.class);
                    requestMethod = verb.value();
                }
                else if(reflectMethod.isAnnotationPresent(PUT.class))
                {
                    PUT verb = reflectMethod.getAnnotation(PUT.class);
                    requestMethod = verb.value();
                }
                else if(reflectMethod.isAnnotationPresent(DELETE.class))
                {
                    DELETE verb = reflectMethod.getAnnotation(DELETE.class);
                    requestMethod = verb.value();
                }
                else if(reflectMethod.isAnnotationPresent(PATCH.class))
                {
                    PATCH verb = reflectMethod.getAnnotation(PATCH.class);
                    requestMethod = verb.value();
                }
                
                if(StringUtils.isBlank(requestMethod))
                {
                    continue;
                }
                
                // allocate the route
                // although request methods are different, it is not concerned if the route exists or not
                Route route = new Route(pathPattern);
                this.router.add(route);

                // store the reflect method with method & pattern joined key
                this.reflectMethods.put(
                    Helper.formatPathPatternKey(requestMethod, pathPattern), 
                    reflectMethod);                
            }
        }
    }
    
    public Datagram route(
            String issuer, String origin, Datagram request, Long receivedTime)
    throws NoSuchElementException
    {
        Datagram result = null;
        
        final String path = request.getHeaderPayload().getPath();
        Route route = this.router.route(path);
        if(null == route)
        {
            throw new NoSuchElementException("404 Not Found");
        }
        
        List<Object> parameters = new ArrayList<>();

        java.lang.reflect.Method reflectMethod = this.reflectMethods.get(
            Helper.formatPathPatternKey(request.getHeaderPayload().getMethod(), route.getResourcePath()));
        // method is not allowed
        if(null == reflectMethod)
        {
            throw new NoSuchElementException("405 Method Not Allowed");
        }
        
        for(java.lang.reflect.Parameter parameter : reflectMethod.getParameters())
        {
            if(parameter.isAnnotationPresent(PathParam.class))
            {
                PathParam pathParam = parameter.getAnnotation(PathParam.class);
                String value = route.getNamedParameter(pathParam.value(), path);
                parameters.add(value);
            }
            else if(parameter.isAnnotationPresent(QueryParam.class))
            {
                
            }
            else if(parameter.isAnnotationPresent(Issuer.class))
            {
                parameters.add(issuer);
            }
            else if(parameter.isAnnotationPresent(Origin.class))
            {
                parameters.add(origin);
            }
            else if(parameter.isAnnotationPresent(Desire.class))
            {
                Desire desire = parameter.getAnnotation(Desire.class);
                parameters.add(request.getBodyPayload().getDesire(desire.type()));
            }
            else if(parameter.isAnnotationPresent(Data.class))
            {
                Data data = parameter.getAnnotation(Data.class);
                parameters.add(request.getBodyPayload().getData(data.type()));
            }
            else if(parameter.isAnnotationPresent(ReceivedTime.class))
            {
                if(parameter.getType().equals(Long.class))
                {
                    parameters.add(receivedTime);
                }
                else
                {
                    parameters.add(null);
                }
            }
            else if(parameter.getType().equals(Datagram.class))
            {
                parameters.add(request);
            }
            else
            {
                parameters.add(null);
            }
        }
        
        Class<?> clazz = reflectMethod.getDeclaringClass();
        try
        {
            Object[] args = parameters.toArray();
            Object returnValue = reflectMethod.invoke(clazz.newInstance(), args);
            if(null != returnValue)
            {
                if(returnValue instanceof Datagram)
                {
                    result = (Datagram) returnValue;
                }
            }
        }
        catch(Throwable t)
        {
            throw new RuntimeException("FAILED TO INVOKE METHOD: " + t.getMessage(), t);
        }
        
        return result;
    }
    
    public static class Helper
    {
        private static final String SPLIT_CHAR = "@";
        public static String formatPathPatternKey(String requestMethod, String pattern)
        {
            return new StringBuilder()
                    .append(requestMethod)
                    .append(SPLIT_CHAR)
                    .append(pattern)
                    .toString();
        }
        
        public static String revertPathPatten(String pathPatternKey)
        {
            int idx = pathPatternKey.indexOf(SPLIT_CHAR);
            if(-1 == idx)
            {
                return pathPatternKey;
            }
            
            return pathPatternKey.substring(idx);
        }
    }
}
