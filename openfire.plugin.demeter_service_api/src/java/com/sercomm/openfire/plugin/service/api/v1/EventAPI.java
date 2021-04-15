package com.sercomm.openfire.plugin.service.api.v1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.AppEventManager;
import com.sercomm.openfire.plugin.OwnershipManager;
import com.sercomm.openfire.plugin.ServiceSessionManager;
import com.sercomm.openfire.plugin.cache.OwnershipCache;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;
import com.sercomm.openfire.plugin.data.frontend.AppEventRecord;
import com.sercomm.openfire.plugin.define.HttpHeader;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;
import com.sercomm.openfire.plugin.service.util.ServiceAPIUtil;

@Path(EventAPI.URI_PATH)
public class EventAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(EventAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v1/";    

    @GET
    @Path("events")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getEvents(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId)
    {
        Response response = null;

        String userId = XStringUtil.BLANK;
        String serial = XStringUtil.BLANK;
        String mac = XStringUtil.BLANK;
        
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
            
            Iterator<String> iterator = ownerships.keySet().iterator();
            while(iterator.hasNext())
            {
                final String nodeName = iterator.next();
                serial = NameRule.toDeviceSerial(nodeName);
                mac = NameRule.toDeviceMac(nodeName);
                break;               
            }

            List<com.sercomm.openfire.plugin.service.dto.v1.Event> entity = 
                    new ArrayList<com.sercomm.openfire.plugin.service.dto.v1.Event>();
            
            List<AppEventRecord> appEventRecords = AppEventManager.getInstance().getAppEventRecords(serial, mac);
            for(AppEventRecord appEventRecord : appEventRecords)
            {
                entity.add(ServiceAPIUtil.V1.convert(appEventRecord));
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
    
    @DELETE
    @Path("events")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteEvents(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId)
    {
        Response response = null;

        String userId = XStringUtil.BLANK;
        String serial = XStringUtil.BLANK;
        String mac = XStringUtil.BLANK;
        
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

            Iterator<String> iterator = ownerships.keySet().iterator();
            while(iterator.hasNext())
            {
                final String nodeName = iterator.next();
                serial = NameRule.toDeviceSerial(nodeName);
                mac = NameRule.toDeviceMac(nodeName);
                break;
            }

            AppEventManager.getInstance().deleteAppEventRecords(serial, mac);            
            
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
}
