package com.sercomm.openfire.plugin.service.api.admin;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import com.sercomm.openfire.plugin.data.frontend.AppCatalog;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.annotation.RequiresRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;
import com.sercomm.openfire.plugin.service.util.ServiceAPIUtil;

@Path(CatalogAPI.URI_PATH)
public class CatalogAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(CatalogAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "admin/";
    
    @GET
    @Path("catalogs")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCatalogs()
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            List<com.sercomm.openfire.plugin.service.dto.admin.Catalog> entity = 
                    new ArrayList<com.sercomm.openfire.plugin.service.dto.admin.Catalog>();
            
            List<AppCatalog> appCatalogs = AppCatalogManager.getInstance().getCatalogs();
            for(AppCatalog appCatalog : appCatalogs)
            {
                final String name = appCatalog.getName();
                
                com.sercomm.openfire.plugin.service.dto.admin.Catalog model = 
                        new com.sercomm.openfire.plugin.service.dto.admin.Catalog();                
                model.id = appCatalog.getId();                
                model.name = name;
                model.created_at = appCatalog.getCreationTime().toString();
                model.count = AppManager.getInstance().getAppCount(name).toString();
                
                entity.add(model);
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
    @Path("catalogs/{id}")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCatalog(
            @PathParam("id") String idString)
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            do
            {
                Integer id = 0;
                try
                {
                    id = Integer.parseInt(idString);
                }
                catch(Throwable ignored)
                {
                    errorMessage = "INVALID ARGUMENT: " + idString;
                    response = createError(
                        Status.BAD_REQUEST,
                        "ERROR",
                        errorMessage);
                    break;
                }
                
                AppCatalog appCatalog = AppCatalogManager.getInstance().getCatalog(id);
                if(null == appCatalog)
                {
                    errorMessage = "CANNOT BE FOUND";
                    response = createError(
                        Status.NOT_FOUND,
                        "ERROR",
                        errorMessage);
                    break;
                }

                com.sercomm.openfire.plugin.service.dto.admin.Catalog entity = 
                        ServiceAPIUtil.Admin.convert(appCatalog);                
                
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
            idString,
            errorMessage);
        
        return response;
    }
    
    @POST
    @Path("catalogs")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response postCatalog(
            String requestPayload)
    {
        Response response = null;
        
        String name = XStringUtil.BLANK;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            do
            {
                try
                {
                    JsonNode rootNode = Json.parse(requestPayload);
                    JsonNode nodeName = rootNode.get("name");
                    
                    name = nodeName.asText(XStringUtil.BLANK);
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
                
                if(XStringUtil.isBlank(name))
                {
                    errorMessage = "ARGUMENT(S) CANNOT BE BLANK";
                    response = createError(
                        Status.UNAUTHORIZED,
                        "ERROR",
                        errorMessage);
                    break;
                }
                
                AppCatalogManager.getInstance().addCatalog(name);

                AppCatalog appCatalog = AppCatalogManager.getInstance().getCatalog(name);
                com.sercomm.openfire.plugin.service.dto.admin.Catalog entity = 
                        ServiceAPIUtil.Admin.convert(appCatalog);
                
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
        
        log.info("({})={}",
            name,
            errorMessage);
        
        return response;
    }
    
    @DELETE
    @Path("catalogs/{id}")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteCatalog(
            @PathParam("id") String idString)
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            do
            {
                Integer id = 0;
                try
                {
                    id = Integer.parseInt(idString);
                }
                catch(Throwable ignored)
                {
                    errorMessage = "INVALID ARGUMENT: " + idString;
                    response = createError(
                        Status.BAD_REQUEST,
                        "ERROR",
                        errorMessage);
                    break;
                }
                
                AppCatalog appCatalog = AppCatalogManager.getInstance().getCatalog(id);
                if(null == appCatalog)
                {
                    errorMessage = "CANNOT BE FOUND";
                    response = createError(
                        Status.NOT_FOUND,
                        "ERROR",
                        errorMessage);
                    break;
                }

                Long count = AppManager.getInstance().getAppCount(appCatalog.getName());
                if(0L != count)
                {
                    errorMessage = "STILL " + count.toString() + " APP(S) WITH THE CATALOG";
                    response = this.createError(
                        Status.FORBIDDEN, 
                        "ERROR", 
                        errorMessage);
                    break;
                }
                
                AppCatalogManager.getInstance().deleteCatalog(appCatalog);
                
                response = Response.status(Status.NO_CONTENT).build();
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
            idString,
            errorMessage);
        
        return response;
    }
}
