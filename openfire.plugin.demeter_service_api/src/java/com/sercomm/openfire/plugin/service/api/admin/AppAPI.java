package com.sercomm.openfire.plugin.service.api.admin;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.AppCatalogManager;
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.data.frontend.AppCatalog;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;
import com.sercomm.openfire.plugin.service.util.ServiceAPIUtil;

@Path(AppAPI.URI_PATH)
public class AppAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(AppAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "admin/";    
    
    @GET
    @Path("apps")
    @RequireRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getApps()
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            List<com.sercomm.openfire.plugin.service.dto.admin.App> entity = 
                    new ArrayList<com.sercomm.openfire.plugin.service.dto.admin.App>();

            List<App> apps = AppManager.getInstance().getApps();
            for(App app : apps)
            {
                com.sercomm.openfire.plugin.service.dto.admin.App object = 
                        ServiceAPIUtil.Admin.convert(app);               
                entity.add(object);
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
    @Path("apps/{appId}")
    @RequireRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getApp(
            @PathParam("appId") String appId)
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            do
            {
                App app = AppManager.getInstance().getApp(appId);                
                if(null == app)
                {
                    throw new DemeterException("CANNOT BE FOUND");
                }
                
                com.sercomm.openfire.plugin.service.dto.admin.App entity = 
                        ServiceAPIUtil.Admin.convert(app);
                
                response = Response.status(Status.OK)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Json.build(entity))
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
        
        log.info("({})={}",
            appId,
            errorMessage);

        return response;
    }

    @POST
    @Path("apps")
    @RequireRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response addApp(
            String payloadString)
    {
        Response response = null;
        
        String publisher = XStringUtil.BLANK;
        String name = XStringUtil.BLANK;
        String catalog = XStringUtil.BLANK;
        String modelName = XStringUtil.BLANK;
        String price = XStringUtil.BLANK;
        Boolean enable = false;
        String description = XStringUtil.BLANK;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            do
            {
                try
                {
                    JsonNode rootNode = Json.parse(payloadString);
                    JsonNode nodePublisher = rootNode.get("company");
                    JsonNode nodeName = rootNode.get("name");
                    JsonNode nodeCatalog = rootNode.get("catalog");
                    JsonNode nodeModelName = rootNode.get("device_model");
                    JsonNode nodePrice = rootNode.get("price");
                    JsonNode nodeEnable = rootNode.get("enable");
                    JsonNode nodeDescription = rootNode.get("desc");
                    
                    publisher = nodePublisher.asText(XStringUtil.BLANK);
                    name = nodeName.asText(XStringUtil.BLANK);
                    catalog = nodeCatalog.asText(XStringUtil.BLANK);
                    modelName = nodeModelName.asText(XStringUtil.BLANK);
                    price = nodePrice.asText(XStringUtil.BLANK);
                    enable = nodeEnable.asBoolean(false);
                    description = nodeDescription.asText(XStringUtil.BLANK);
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

                int publish = false == enable ? 0 : 1;
                
                AppManager.getInstance().addApp(
                    publisher, 
                    name, 
                    catalog, 
                    modelName,
                    price, 
                    publish, 
                    description, 
                    null,
                    null,
                    null);
                
                App app = AppManager.getInstance().getApp(publisher, name, modelName);
                com.sercomm.openfire.plugin.service.dto.admin.App entity = 
                        ServiceAPIUtil.Admin.convert(app);
                
                response = Response.status(Status.CREATED)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Json.build(entity))
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
        
        log.info("({},{},{},{},{},{},{})={}",
            publisher,
            name,
            catalog,
            modelName,
            price,
            enable,
            description,
            errorMessage);

        return response;
    }

    @PUT
    @Path("apps/{appId}")
    @RequireRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response setApp(
            @PathParam("appId") String appId,
            String payloadString)
    {
        Response response = null;
        
        String publisher = XStringUtil.BLANK;
        String catalogString = XStringUtil.BLANK;
        String price = XStringUtil.BLANK;
        String serial = XStringUtil.BLANK;
        Boolean enable = false;
        String description = XStringUtil.BLANK;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            do
            {
                try
                {
                    // NOTICE: modify App's name is not allowed
                    // since App's name will be container's name in the future
                    
                    JsonNode rootNode = Json.parse(payloadString);
                    JsonNode nodePublisher = rootNode.get("company");
                    JsonNode nodeCatalog = rootNode.get("catalog");
                    JsonNode nodePrice = rootNode.get("price");
                    JsonNode nodeSerial = rootNode.get("serial");
                    JsonNode nodeEnable = rootNode.get("enable");
                    JsonNode nodeDescription = rootNode.get("desc");
                    
                    publisher = nodePublisher.asText(XStringUtil.BLANK);
                    catalogString = nodeCatalog.asText(XStringUtil.BLANK);
                    price = nodePrice.asText(XStringUtil.BLANK);
                    serial = nodeSerial.asText(XStringUtil.BLANK);
                    enable = nodeEnable.asBoolean(false);
                    description = nodeDescription.asText(XStringUtil.BLANK);
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

                AppCatalog catalog = AppCatalogManager.getInstance().getCatalog(catalogString);
                if(null == catalog)
                {
                    throw new DemeterException("INVALID CATALOG");
                }
                
                int publish = false == enable ? 0 : 1;
                app.setCatalog(catalog.getName());
                app.setPrice(price);
                app.setPublish(publish);
                app.setDescription(description);
                
                AppManager.getInstance().setApp(
                    app, 
                    null,
                    null,
                    null);
                
                com.sercomm.openfire.plugin.service.dto.admin.App entity = 
                        ServiceAPIUtil.Admin.convert(app);
                
                response = Response.status(Status.OK)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Json.build(entity))
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
        
        log.info("({},{},{},{},{},{})={}",
            publisher,
            catalogString,
            price,
            serial,
            enable,
            description,
            errorMessage);

        return response;
    }

    @DELETE
    @Path("apps/{appId}")
    @RequireRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteApp(
            @PathParam("appId") String appId)
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            do
            {
                App app = AppManager.getInstance().getApp(appId);                
                if(null == app)
                {
                    errorMessage = "CANNOT BE FOUND";
                    response = createError(
                        Status.NOT_FOUND,
                        "ERROR",
                        errorMessage);
                    break;                
                }
                
                AppManager.getInstance().deleteApp(appId);
                
                response = Response.status(Status.NO_CONTENT).build();
            }
            while(false);            
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();
            Status http_status = e.GetHttpStatus();
            response = createError(
                http_status != null ? http_status : Status.FORBIDDEN,
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
            appId,
            errorMessage);

        return response;
    }

    @POST
    @Path("apps/{appId}/files") // TODO: rename "files" to "icon" from WEB page
    @RequireRoles({EndUserRole.ADMIN})
    @Consumes(MediaType.APPLICATION_JSON) //TODO: WEB page bug
    @Produces({MediaType.APPLICATION_JSON})
    public Response postIcon(
            @PathParam("appId") String appId,
            byte[] requestPayload)
    {
        Response response = null;

        int contentLength = requestPayload.length;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            do
            {
                App app = AppManager.getInstance().getApp(appId);
                if(null == app)
                {
                    errorMessage = "CANNOT BE FOUND";
                    response = createError(
                        Status.NOT_FOUND,
                        "ERROR",
                        errorMessage);
                    break;                
                }
                
                AppManager.getInstance().setApp(app, null, null, requestPayload);
                
                response = Response.status(Status.CREATED).build();
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
        
        log.info("({},{})={}",
            appId,
            contentLength,
            errorMessage);

        return response;
    }

    @PUT
    @Path("apps/{appId}/files/{iconId}") // TODO: rename "files" to "icon" from WEB page
    @RequireRoles({EndUserRole.ADMIN})
    @Consumes(MediaType.APPLICATION_JSON) //TODO: WEB page bug
    @Produces({MediaType.APPLICATION_JSON})
    public Response putIcon(
            @PathParam("appId") String appId,
            @PathParam("iconId") String iconId,
            byte[] requestPayload)
    {
        Response response = null;

        int contentLength = requestPayload.length;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            do
            {
                App app = AppManager.getInstance().getApp(appId);
                if(null == app)
                {
                    errorMessage = "CANNOT BE FOUND";
                    response = createError(
                        Status.NOT_FOUND,
                        "ERROR",
                        errorMessage);
                    break;                
                }
                
                AppManager.getInstance().setApp(app, null, null, requestPayload);
                
                response = Response.status(Status.OK).build();
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
        
        log.info("({},{})={}",
            appId,
            contentLength,
            errorMessage);

        return response;
    }

    @POST
    @Path("apps/{appId}/opkg") // TODO: rename "files" to "ipk" from WEB page
    @RequireRoles({EndUserRole.ADMIN})
    @Consumes(MediaType.APPLICATION_JSON) //TODO: WEB page bug
    @Produces({MediaType.APPLICATION_JSON})
    public Response postOPKG(
            @PathParam("appId") String appId,
            byte[] requestPayload)
    {
        Response response = null;

        int contentLength = requestPayload.length;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            AppManager.getInstance().addAppVersion(
                appId,
                null,
                1,
                XStringUtil.BLANK,
                requestPayload);
            
            response = Response.status(Status.CREATED).build();
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();
            Status status = e.GetHttpStatus();
            response = createError(
                status != null ? status : Status.FORBIDDEN,
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
        
        log.info("({},{})={}",
            appId,
            contentLength,
            errorMessage);

        return response;
    }
}
