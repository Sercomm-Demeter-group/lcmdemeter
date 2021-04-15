package com.sercomm.openfire.plugin.service.api.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.data.frontend.AppVersion;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.annotation.RequiresRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;
import com.sercomm.openfire.plugin.service.util.ServiceAPIUtil;

@Path(DeviceAPI.URI_PATH)
public class DeviceAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(DeviceAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "admin/";    

    @GET
    @Path("devices")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDevices()
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            List<com.sercomm.openfire.plugin.service.dto.admin.Device> entity = 
                    new ArrayList<com.sercomm.openfire.plugin.service.dto.admin.Device>();
            
            Collection<User> devices = UserManager.getInstance().getUsers();
            for(User device : devices)
            {
                final String nodeName = device.getUsername();
                if(false == NameRule.isDevice(nodeName))
                {
                    continue;
                }
                
                final String serial = NameRule.toDeviceSerial(nodeName);
                final String mac = NameRule.toDeviceMac(nodeName);
                
                DeviceCache deviceCache = DeviceManager.getInstance().getDeviceCache(serial, mac);                
                entity.add(ServiceAPIUtil.Admin.convert(deviceCache));
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
            log.error(t.getMessage(), t);
            response = createError(
                Status.INTERNAL_SERVER_ERROR,
                "INTERNAL SERVER ERROR",
                errorMessage);
        }        
        
        log.info("()={}",
            errorMessage);

        return response;
    }

    @GET
    @Path("devices/{deviceId}")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDevice(
            @PathParam("deviceId") String deviceId)
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            do
            {
                if(false == UserManager.getInstance().isRegisteredUser(deviceId))
                {
                    throw new DemeterException("DEVICE CANNOT BE FOUND");
                }

                final String serial = NameRule.toDeviceSerial(deviceId);
                final String mac = NameRule.toDeviceMac(deviceId);

                DeviceCache deviceCache = DeviceManager.getInstance().getDeviceCache(serial, mac);
                                
                response = Response.status(Status.OK)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Json.build(ServiceAPIUtil.Admin.convert(deviceCache)))
                        .build();
            }
            while(false);
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
            log.error(t.getMessage(), t);
            response = createError(
                Status.INTERNAL_SERVER_ERROR,
                "INTERNAL SERVER ERROR",
                errorMessage);
        }        
        
        log.info("()={}",
            errorMessage);

        return response;
    }

    @PUT
    @Path("devices/{deviceId}")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateDevice(
            @PathParam("deviceId") String deviceId,
            String requestPayload)
    {
        Response response = null;
        
        String company = XStringUtil.BLANK;
        String name = XStringUtil.BLANK;
        Boolean enable = false;

        String errorMessage = XStringUtil.BLANK;        
        try
        {
            do
            {
                try
                {
                    JsonNode rootNode = Json.parse(requestPayload);
                    JsonNode nodeCompany = rootNode.get("company");
                    JsonNode nodeName = rootNode.get("name");
                    JsonNode nodeEnable = rootNode.get("enable");
                    
                    company = nodeCompany.asText(XStringUtil.BLANK);
                    name = nodeName.asText(XStringUtil.BLANK);
                    enable = nodeEnable.asBoolean(false);
                }
                catch(Throwable ignored)
                {
                    errorMessage = "INVALID ARGUMENT FORMAT";
                    response = createError(
                        Status.UNAUTHORIZED,
                        "ERROR",
                        errorMessage);
                    break;                
                }
                
                final String serial = NameRule.toDeviceSerial(deviceId);
                final String mac = NameRule.toDeviceMac(deviceId);

                DeviceCache deviceCache;
                Lock locker = DeviceManager.getInstance().getLock(serial, mac);
                try
                {
                    locker.lock();
                    
                    deviceCache = DeviceManager.getInstance().getDeviceCache(serial, mac);                   
                    deviceCache.setCompany(company);
                    deviceCache.setCustomName(name);
                    deviceCache.setEnable(enable);
                    
                    deviceCache.flush();
                }
                finally
                {
                    locker.unlock();
                }
                                
                response = Response.status(Status.CREATED)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Json.build(ServiceAPIUtil.Admin.convert(deviceCache)))
                        .build();
            }
            while(false);
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
            log.error(t.getMessage(), t);
            response = createError(
                Status.INTERNAL_SERVER_ERROR,
                "INTERNAL SERVER ERROR",
                errorMessage);
        }        
        
        log.info("({},{},{},{})={}",
            deviceId,
            company,
            name,
            enable,
            errorMessage);

        return response;
    }

    @GET
    @Path("devices/{deviceId}/apps")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDeviceApps(
            @PathParam("deviceId") String deviceId)
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            final String serial = NameRule.toDeviceSerial(deviceId);
            final String mac = NameRule.toDeviceMac(deviceId);
            
            List<com.sercomm.openfire.plugin.service.dto.admin.App> entity =
                    new ArrayList<com.sercomm.openfire.plugin.service.dto.admin.App>();
            
            List<String> collection = DeviceManager.getInstance().getInstalledVersionIds(serial, mac);
            // collection of App version ID
            for(String versionId : collection)
            {
                AppVersion appVersion = AppManager.getInstance().getAppVersion(versionId);
                if(null == appVersion)
                {
                    continue;
                }
                
                App app = AppManager.getInstance().getApp(appVersion.getAppId());
                if(null == app)
                {
                    continue;
                }
                
                entity.add(ServiceAPIUtil.Admin.convert(app));
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
            log.error(t.getMessage(), t);
            response = createError(
                Status.INTERNAL_SERVER_ERROR,
                "INTERNAL SERVER ERROR",
                errorMessage);
        }                
        
        log.info("({})={}",
            deviceId,
            errorMessage);

        return response;
    }
    
    @DELETE
    @Path("devices/{deviceId}")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteDevice(
            @PathParam("deviceId") String deviceId)
    {
        // SHOULD NOT BE DELETED
        Response response = createError(
            Status.FORBIDDEN,
            "ERROR",
            "DEVICE CANNOT BE REMOVED");
        return response;
    }

    @GET
    @Path("devices/{deviceId}/usage")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDeviceUsage(
            @PathParam("deviceId") String deviceId)
    {
        // TODO
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }
}
