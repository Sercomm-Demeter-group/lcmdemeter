package com.sercomm.openfire.plugin.service.api.v1;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.Log;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.EndUserManager;
import com.sercomm.openfire.plugin.OwnershipManager;
import com.sercomm.openfire.plugin.ServiceSessionManager;
import com.sercomm.openfire.plugin.cache.EndUserCache;
import com.sercomm.openfire.plugin.cache.OwnershipCache;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.define.HttpHeader;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(AuthAPI.URI_PATH)
public class AuthAPI extends ServiceAPIBase
{
    protected final static String URI_PATH = ServiceAPIBase.URI_PATH + "v1/";    
    
    @Context 
    private HttpServletRequest request;

    @POST
    @Path("auth")
    @Produces({MediaType.APPLICATION_JSON})
    public Response authenticate(
            @QueryParam("full") Boolean full,
            String requestPayload)
    {
        Response response = null;

        String customer = XStringUtil.BLANK;
        String password = XStringUtil.BLANK;
        String device = XStringUtil.BLANK;              

        String errorMessage = XStringUtil.BLANK;        
        try
        {
            String sessionId = request.getHeader(HttpHeader.X_AUTH_TOKEN);
            if(XStringUtil.isBlank(sessionId))
            {
                sessionId = UUID.randomUUID().toString();
            }
            
            try
            {
                JsonNode rootNode = Json.parse(requestPayload);
                JsonNode nodeCustomer = rootNode.get("customer");
                JsonNode nodePassword = rootNode.get("password");
                JsonNode nodeDevice = rootNode.get("device");
                
                customer = nodeCustomer.asText(XStringUtil.BLANK);
                password = nodePassword.asText(XStringUtil.BLANK);
                device = nodeDevice.asText(XStringUtil.BLANK);                    
            }
            catch(Throwable ignored)
            {
                errorMessage = "INVALID ARGUMENT FORMAT";
                throw new DemeterException(errorMessage);
            }

            if(XStringUtil.isBlank(customer) || XStringUtil.isBlank(password))
            {
                errorMessage = "ARGUMENT(S) CANNOT BE BLANK";
                throw new DemeterException(errorMessage);
            }

            if(false == EndUserManager.getInstance().isRegisteredUser(customer))
            {
                errorMessage = "INVALID USERNAME OR PASSWORD";
                throw new DemeterException(errorMessage);
            }

            String plainPassword = EndUserManager.getInstance().getPassword(customer);
            if(0 != plainPassword.compareTo(password))
            {
                errorMessage = "INVALID USERNAME OR PASSWORD";
                throw new DemeterException(errorMessage);
            }

            EndUserCache endUserCache = EndUserManager.getInstance().getUser(customer);
            EndUserRole endUserRole = endUserCache.getEndUserRole();
            
            ServiceSessionCache cache = new ServiceSessionCache();
            cache.setSessionId(sessionId);
            cache.setUserId(customer);
            cache.setEndUserRole(endUserRole);
            ServiceSessionManager.getInstance().updateSession(cache);

            Map<String, String> entity = new HashMap<String, String>();
            entity.put("customer", customer);
            switch(endUserRole)
            {
                case ADMIN:
                    entity.put("device", (0 == EndUserRole.ADMIN.compareTo(endUserRole)) ? EndUserRole.ADMIN.toString() : XStringUtil.BLANK);
                    break;
                case MEMBER:
                    {
                        Map<String, OwnershipCache> ownerships = OwnershipManager.getInstance().getOwnerships(customer);

                        String serial = XStringUtil.BLANK;
                        Iterator<String> iterator = ownerships.keySet().iterator();
                        while(iterator.hasNext())
                        {
                            serial = NameRule.toDeviceSerial(iterator.next());
                            break;
                        }
                        entity.put("device", serial);
                    }
                    break;
            }
            
            response = Response.status(Status.OK)
                    .header(HttpHeader.X_AUTH_TOKEN, sessionId)
                    .header(HttpHeader.X_ROLES, (0 == EndUserRole.ADMIN.compareTo(endUserRole)) ? "administrator" : EndUserRole.MEMBER.toString())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Json.build(entity))
                    .build();
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();
            response = createError(
                Status.FORBIDDEN,
                "UNAUTHORIZED",
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
        
        Log.write().info("({},{},{})={}",
            customer,
            password,
            device,
            errorMessage);
        
        return response;
    }
}
