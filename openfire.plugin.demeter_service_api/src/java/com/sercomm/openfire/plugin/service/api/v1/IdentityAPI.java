package com.sercomm.openfire.plugin.service.api.v1;

import java.util.Iterator;
import java.util.Map;

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
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.EndUserManager;
import com.sercomm.openfire.plugin.OwnershipManager;
import com.sercomm.openfire.plugin.ServiceSessionManager;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.cache.EndUserCache;
import com.sercomm.openfire.plugin.cache.OwnershipCache;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;
import com.sercomm.openfire.plugin.define.HttpHeader;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;
import com.sercomm.openfire.plugin.service.util.ServiceAPIUtil;

@Path(IdentityAPI.URI_PATH)
public class IdentityAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(IdentityAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v1/";    

    @GET
    @Path("identity")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getEndUser(
            @HeaderParam(HttpHeader.X_AUTH_TOKEN) String sessionId)
    {
        Response response = null;
        
        String userId = XStringUtil.BLANK;
        
        String errorMessage = XStringUtil.BLANK;
        try
        {
            ServiceSessionCache session = ServiceSessionManager.getInstance().getSession(sessionId);
            userId = session.getUserId();

            EndUserCache endUserCache = EndUserManager.getInstance().getUser(userId);
            Map<String, OwnershipCache> ownerships = OwnershipManager.getInstance().getOwnerships(userId);
            
            DeviceCache deviceCache = null;
            Iterator<String> iterator = ownerships.keySet().iterator();
            while(iterator.hasNext())
            {
                final String nodeName = iterator.next();
                final String serial = NameRule.toDeviceSerial(nodeName);
                final String mac = NameRule.toDeviceMac(nodeName);
                
                deviceCache = DeviceManager.getInstance().getDeviceCache(serial, mac);
                break;
            }
            
            response = Response.status(Status.OK)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Json.build(ServiceAPIUtil.V1.convert(endUserCache, deviceCache)))
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
}
