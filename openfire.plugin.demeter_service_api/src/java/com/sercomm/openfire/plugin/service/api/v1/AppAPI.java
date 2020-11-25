package com.sercomm.openfire.plugin.service.api.v1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.Log;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.AppEventManager;
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.OwnershipManager;
import com.sercomm.openfire.plugin.ServiceSessionManager;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.cache.OwnershipCache;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.data.frontend.AppSubscription;
import com.sercomm.openfire.plugin.define.AppAction;
import com.sercomm.openfire.plugin.define.AppEventType;
import com.sercomm.openfire.plugin.define.DeviceState;
import com.sercomm.openfire.plugin.define.HttpHeader;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;
import com.sercomm.openfire.plugin.service.util.ServiceAPIUtil;

@Path(AppAPI.URI_PATH)
public class AppAPI extends ServiceAPIBase
{
    protected final static String URI_PATH = ServiceAPIBase.URI_PATH + "v1/";    

    @GET
    @Path("apps")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getApps(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId,
            @QueryParam("purchased") Boolean purchased)
    {
        Response response = null;
        
        String userId = XStringUtil.BLANK;
        
        String errorMessage = XStringUtil.BLANK;
        try
        {
            List<com.sercomm.openfire.plugin.service.dto.v1.App> entity = 
                    new ArrayList<com.sercomm.openfire.plugin.service.dto.v1.App>();

            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            userId = session.getUserId();

            Map<String, OwnershipCache> ownerships = OwnershipManager.getInstance().getOwnerships(userId);
            Iterator<String> iterator = ownerships.keySet().iterator();
            while(iterator.hasNext())
            {
                final String nodeName = iterator.next();
                final String serial = NameRule.toDeviceSerial(nodeName);
                final String mac = NameRule.toDeviceMac(nodeName);
                
                DeviceCache deviceCache = DeviceManager.getInstance().getDeviceCache(serial, mac);
                if(null == deviceCache)
                {
                    throw new DemeterException("DEVICE CANNOT BE FOUND");
                }

                if(0 != DeviceState.ONLINE.compareTo(deviceCache.getDeviceState()))
                {
                    throw new DemeterException("DEVICE IS UNAVAILABLE TEMPORARILY");
                }

                final String modelName = deviceCache.getModelName();

                if(null == purchased || false == purchased)
                {
                    List<App> apps = AppManager.getInstance().getAppsByModel(modelName);
                    for(App app : apps)
                    {
                        com.sercomm.openfire.plugin.service.dto.v1.App object = 
                                ServiceAPIUtil.V1.convert(app, userId, serial, mac);
                        entity.add(object);
                    }
                }
                else if(null != purchased && true == purchased)
                {
                    List<AppSubscription> collection = AppManager.getInstance().getSubscribedApps(userId);
                    for(AppSubscription appSubscription : collection)
                    {
                        App app = AppManager.getInstance().getApp(appSubscription.getAppId());
                        if(null == app)
                        {
                            // the App has already been deleted
                            // it needs to be unsubscribed by the user
                            AppManager.getInstance().unsubscribeApp(appSubscription.getAppId(), userId);
                            continue;
                        }
                        
                        com.sercomm.openfire.plugin.service.dto.v1.App object = 
                                ServiceAPIUtil.V1.convert(app, userId, serial, mac);
                        entity.add(object);
                    }
                }

                break;
            }
            
            response = Response.status(Status.OK)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Json.build(entity))
                    .build();
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();
            response = createError(
                Status.FORBIDDEN,
                "ERROR",
                errorMessage);
        }
        catch(Throwable t)
        {
            errorMessage = t.getMessage();
            Log.write().error(t.getMessage(), t);

            response = createError(
                Status.INTERNAL_SERVER_ERROR,
                "INTERNAL SERVER ERROR",
                errorMessage);
        }
        
        Log.write().info("({},{})={}",
            userId,
            purchased,
            errorMessage);
        
        return response;
    }

    @GET
    @Path("apps/{appId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getApp(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId,
            @PathParam("appId") String appId)
    {
        Response response = null;
                
        String userId = XStringUtil.BLANK;
        
        String errorMessage = XStringUtil.BLANK;
        try
        {
            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            userId = session.getUserId();

            Map<String, OwnershipCache> ownerships = OwnershipManager.getInstance().getOwnerships(userId);
            if(ownerships.isEmpty())
            {
                throw new DemeterException("NO DEVICE AVAILABLE");
            }

            App app = AppManager.getInstance().getApp(appId);
            if(null == app)
            {
                throw new DemeterException("APP CANNOT BE FOUND");
            }
            
            com.sercomm.openfire.plugin.service.dto.v1.App entity = null;
            
            Iterator<String> iterator = ownerships.keySet().iterator();
            while(iterator.hasNext())
            {
                final String nodeName = iterator.next();
                final String serial = NameRule.toDeviceSerial(nodeName);
                final String mac = NameRule.toDeviceMac(nodeName);
                
                entity = ServiceAPIUtil.V1.convert(app, userId, serial, mac);
                break;
            }

            response = Response.status(Status.OK)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Json.build(entity))
                    .build();
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();
            response = createError(
                Status.FORBIDDEN,
                "ERROR",
                errorMessage);
        }
        catch(Throwable t)
        {
            errorMessage = t.getMessage();
            Log.write().error(t.getMessage(), t);

            response = createError(
                Status.INTERNAL_SERVER_ERROR,
                "INTERNAL SERVER ERROR",
                errorMessage);
        }
        
        Log.write().info("({},{})={}",
            userId,
            appId,
            errorMessage);
        
        return response;
    }
    
    @POST
    @Path("apps/{appId}/purchase")
    @Produces({MediaType.APPLICATION_JSON})
    public Response subscribeApp(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId,
            @PathParam("appId") String appId)
    {
        Response response = null;
        
        String userId = XStringUtil.BLANK;
        String appName = XStringUtil.BLANK;
        String serial = XStringUtil.BLANK;
        String mac = XStringUtil.BLANK;
        
        String errorMessage = XStringUtil.BLANK;
        try
        {
            App app = AppManager.getInstance().getApp(appId);
            if(null == app)
            {
                throw new DemeterException("APP CANNOT BE FOUND");
            }
            appName = app.getName();
            
            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            userId = session.getUserId();

            Map<String, OwnershipCache> ownerships = OwnershipManager.getInstance().getOwnerships(userId);
            if(ownerships.isEmpty())
            {
                throw new DemeterException("NO DEVICE AVAILABLE");
            }
            
            Iterator<String> iterator = ownerships.keySet().iterator();
            while(iterator.hasNext())
            {
                final String nodeName = iterator.next();
                serial = NameRule.toDeviceSerial(nodeName);
                mac = NameRule.toDeviceMac(nodeName);
                break;               
            }
            
            AppManager.getInstance().subscribeApp(appId, userId);
            AppEventManager.getInstance().triggerAppEvent(
                appId, 
                appName, 
                serial, 
                mac, 
                userId, 
                AppEventType.SUBSCRIBE);
            
            response = Response.status(Status.CREATED).build();
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();
            response = createError(
                Status.FORBIDDEN,
                "ERROR",
                errorMessage);
        }
        catch(Throwable t)
        {
            errorMessage = t.getMessage();
            Log.write().error(t.getMessage(), t);

            response = createError(
                Status.INTERNAL_SERVER_ERROR,
                "INTERNAL SERVER ERROR",
                errorMessage);
        }
        
        Log.write().info("({},{})={}",
            userId,
            appId,
            errorMessage);
        
        return response;
    }

    @DELETE
    @Path("apps/{appId}/purchase")
    @Produces({MediaType.APPLICATION_JSON})
    public Response unsubscribeApp(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId,
            @PathParam("appId") String appId)
    {
        Response response = null;

        String userId = XStringUtil.BLANK;
        String appName = XStringUtil.BLANK;
        String serial = XStringUtil.BLANK;
        String mac = XStringUtil.BLANK;

        String errorMessage = XStringUtil.BLANK;
        try
        {
            App app = AppManager.getInstance().getApp(appId);
            if(null == app)
            {
                throw new DemeterException("APP CANNOT BE FOUND");
            }
            appName = app.getName();

            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            userId = session.getUserId();

            Map<String, OwnershipCache> ownerships = OwnershipManager.getInstance().getOwnerships(userId);
            if(ownerships.isEmpty())
            {
                throw new DemeterException("NO DEVICE AVAILABLE");
            }
            
            Iterator<String> iterator = ownerships.keySet().iterator();
            while(iterator.hasNext())
            {
                final String nodeName = iterator.next();
                serial = NameRule.toDeviceSerial(nodeName);
                mac = NameRule.toDeviceMac(nodeName);
                break;               
            }

            AppManager.getInstance().unsubscribeApp(appId, userId);
            AppEventManager.getInstance().triggerAppEvent(
                appId, 
                appName, 
                serial, 
                mac, 
                userId, 
                AppEventType.UNSUBSCRIBE);
            
            response = Response.status(Status.NO_CONTENT).build();
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();
            response = createError(
                Status.FORBIDDEN,
                "ERROR",
                errorMessage);
        }
        catch(Throwable t)
        {
            errorMessage = t.getMessage();
            Log.write().error(t.getMessage(), t);

            response = createError(
                Status.INTERNAL_SERVER_ERROR,
                "INTERNAL SERVER ERROR",
                errorMessage);
        }
        
        Log.write().info("({},{})={}",
            userId,
            appId,
            errorMessage);
        
        return response;
    }

    @POST
    @Path("apps")
    @Produces({MediaType.APPLICATION_JSON})
    public Response installApp(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId,
            String payloadString)
    {
        Response response = null;
        
        String userId = XStringUtil.BLANK;
        String appId = XStringUtil.BLANK;
        Boolean enable = false;

        String appName = XStringUtil.BLANK;
        String serial = XStringUtil.BLANK;
        String mac = XStringUtil.BLANK;
        
        String errorMessage = XStringUtil.BLANK;
        try
        {
            try
            {
                JsonNode rootNode = Json.parse(payloadString);
                JsonNode nodeAppId = rootNode.get("app_id");
                JsonNode nodeEnable = rootNode.get("enable");
                
                appId = nodeAppId.asText(XStringUtil.BLANK);
                enable = nodeEnable.asBoolean(false);
            }
            catch(Throwable ignored)
            {
                throw new DemeterException("INVALID ARGUMENT FORMAT");
            }
            
            App app = AppManager.getInstance().getApp(appId);
            if(null == app)
            {
                throw new DemeterException("APP CANNOT BE FOUND");
            }
            appName = app.getName();

            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            userId = session.getUserId();

            Map<String, OwnershipCache> ownerships = OwnershipManager.getInstance().getOwnerships(userId);
            if(ownerships.isEmpty())
            {
                throw new DemeterException("NO DEVICE AVAILABLE");
            }
            
            Iterator<String> iterator = ownerships.keySet().iterator();
            while(iterator.hasNext())
            {
                final String nodeName = iterator.next();
                serial = NameRule.toDeviceSerial(nodeName);
                mac = NameRule.toDeviceMac(nodeName);
                break;
            }

            DeviceManager.getInstance().installApp(serial, mac, appId);                
            AppEventManager.getInstance().triggerAppEvent(
                appId, 
                appName, 
                serial, 
                mac, 
                userId, 
                AppEventType.INSTALL);

            response = Response.status(Status.OK).build();
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();
            response = createError(
                Status.FORBIDDEN,
                "ERROR",
                errorMessage);
        }
        catch(Throwable t)
        {
            errorMessage = t.getMessage();
            Log.write().error(t.getMessage(), t);

            response = createError(
                Status.INTERNAL_SERVER_ERROR,
                "INTERNAL SERVER ERROR",
                errorMessage);
        }
        
        Log.write().info("({},{},{},{},{})={}",
            userId,
            serial,
            mac,
            appId,
            enable,
            errorMessage);
        
        return response;
    }

    @DELETE
    @Path("apps/{appId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response uninstallApp(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId,
            @PathParam("appId") String appId,
            String payloadString)
    {
        Response response = null;

        String userId = XStringUtil.BLANK;
        String appName = XStringUtil.BLANK;
        String serial = XStringUtil.BLANK;
        String mac = XStringUtil.BLANK;
                
        String errorMessage = XStringUtil.BLANK;
        try
        {            
            App app = AppManager.getInstance().getApp(appId);
            if(null == app)
            {
                throw new DemeterException("APP CANNOT BE FOUND");
            }
            appName = app.getName();

            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            userId = session.getUserId();

            Map<String, OwnershipCache> ownerships = OwnershipManager.getInstance().getOwnerships(userId);
            if(ownerships.isEmpty())
            {
                throw new DemeterException("NO DEVICE AVAILABLE");
            }
            
            Iterator<String> iterator = ownerships.keySet().iterator();
            while(iterator.hasNext())
            {
                final String nodeName = iterator.next();
                serial = NameRule.toDeviceSerial(nodeName);
                mac = NameRule.toDeviceMac(nodeName);
                break;
            }

            DeviceManager.getInstance().uninstallApp(serial, mac, appId);                
            AppEventManager.getInstance().triggerAppEvent(
                appId, 
                appName, 
                serial, 
                mac, 
                userId, 
                AppEventType.UNINSTALL);

            response = Response.status(Status.NO_CONTENT).build();
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();
            response = createError(
                Status.FORBIDDEN,
                "ERROR",
                errorMessage);
        }
        catch(Throwable t)
        {
            errorMessage = t.getMessage();
            Log.write().error(t.getMessage(), t);

            response = createError(
                Status.INTERNAL_SERVER_ERROR,
                "INTERNAL SERVER ERROR",
                errorMessage);
        }
        
        Log.write().info("({},{},{},{})={}",
            userId,
            serial,
            mac,
            appId,
            errorMessage);
        
        return response;
    }
    
    @PUT
    @Path("apps/{appId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response controlApp(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId,
            @PathParam("appId") String appId,
            String payloadString)
    {
        Response response = null;
        
        String userId = XStringUtil.BLANK;
        String appName = XStringUtil.BLANK;
        String serial = XStringUtil.BLANK;
        String mac = XStringUtil.BLANK;
        
        String actionString = XStringUtil.BLANK;

        String errorMessage = XStringUtil.BLANK;
        try
        {
            try
            {
                JsonNode rootNode = Json.parse(payloadString);
                JsonNode nodeAction = rootNode.get("action");
                
                actionString = nodeAction.asText(XStringUtil.BLANK);
            }
            catch(Throwable ignored)
            {
                throw new DemeterException("INVALID ARGUMENT FORMAT");
            }

            if(XStringUtil.isBlank(actionString))
            {
                throw new DemeterException("ARGUMENT(S) CANNOT BE BLANK");
            }
            
            AppAction appAction = AppAction.fromString(actionString);
            if(null == appAction)
            {
                throw new DemeterException("INVALID ARGUMENT: " + actionString);
            }
            
            App app = AppManager.getInstance().getApp(appId);
            if(null == app)
            {
                throw new DemeterException("APP CANNOT BE FOUND");
            }
            appName = app.getName();

            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            userId = session.getUserId();

            Map<String, OwnershipCache> ownerships = OwnershipManager.getInstance().getOwnerships(userId);
            if(ownerships.isEmpty())
            {
                throw new DemeterException("NO DEVICE AVAILABLE");
            }
            
            Iterator<String> iterator = ownerships.keySet().iterator();
            while(iterator.hasNext())
            {
                final String nodeName = iterator.next();
                
                serial = NameRule.toDeviceSerial(nodeName);
                mac = NameRule.toDeviceMac(nodeName);
                break;
            }

            DeviceManager.getInstance().controlApp(appId, serial, mac, appAction);
            AppEventManager.getInstance().triggerAppEvent(
                appId, 
                appName, 
                serial, 
                mac, 
                userId, 
                0 == appAction.compareTo(AppAction.START) ? AppEventType.START : AppEventType.STOP);
            
            response = Response.status(Status.CREATED).build();
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();
            response = createError(
                Status.FORBIDDEN,
                "ERROR",
                errorMessage);
        }
        catch(Throwable t)
        {
            errorMessage = t.getMessage();
            Log.write().error(t.getMessage(), t);

            response = createError(
                Status.INTERNAL_SERVER_ERROR,
                "INTERNAL SERVER ERROR",
                errorMessage);
        }
        
        Log.write().info("({},{},{},{},{})={}",
            userId,
            serial,
            mac,
            appId,
            actionString,
            errorMessage);
        
        return response;
    }
}
