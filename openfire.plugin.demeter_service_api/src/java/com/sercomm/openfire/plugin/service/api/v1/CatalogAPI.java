package com.sercomm.openfire.plugin.service.api.v1;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.AppCatalogManager;
import com.sercomm.openfire.plugin.ServiceSessionManager;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;
import com.sercomm.openfire.plugin.data.frontend.AppCatalog;
import com.sercomm.openfire.plugin.define.HttpHeader;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;
import com.sercomm.openfire.plugin.service.util.ServiceAPIUtil;

@Path(CatalogAPI.URI_PATH)
public class CatalogAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(CatalogAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v1/";
    
    @GET
    @Path("catalogs")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAppCatalogs(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId)
    {
        Response response = null;
        
        String userId = XStringUtil.BLANK;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            userId = session.getUserId();

            List<com.sercomm.openfire.plugin.service.dto.v1.Catalog> entity = 
                    new ArrayList<com.sercomm.openfire.plugin.service.dto.v1.Catalog>();
            
            List<AppCatalog> appCatalogs = AppCatalogManager.getInstance().getCatalogs();
            for(AppCatalog appCatalog : appCatalogs)
            {
                entity.add(ServiceAPIUtil.V1.convert(appCatalog));
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
            userId,
            errorMessage);
        
        return response;
    }
    
    @GET
    @Path("catalogs/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCatalog(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId,
            @PathParam("id") String idString)
    {
        Response response = null;
        
        String userId = XStringUtil.BLANK;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            userId = session.getUserId();

            Integer id = 0;
            try
            {
                id = Integer.parseInt(idString);
            }
            catch(Throwable ignored)
            {
                errorMessage = "INVALID ARGUMENT: " + idString;
                throw new DemeterException(errorMessage);
            }
            
            AppCatalog appCatalog = AppCatalogManager.getInstance().getCatalog(id);
            if(null == appCatalog)
            {
                errorMessage = "CANNOT BE FOUND";
                throw new DemeterException(errorMessage);
            }
            
            response = Response.status(Status.OK)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Json.build(ServiceAPIUtil.Admin.convert(appCatalog)))
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
        
        log.info("({},{})={}",
            userId,
            idString,
            errorMessage);
        
        return response;
    }
}
