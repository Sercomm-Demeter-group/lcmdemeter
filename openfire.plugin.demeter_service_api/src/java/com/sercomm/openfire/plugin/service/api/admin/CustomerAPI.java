package com.sercomm.openfire.plugin.service.api.admin;

import java.util.ArrayList;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.EndUserManager;
import com.sercomm.openfire.plugin.cache.EndUserCache;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.annotation.RequiresRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;
import com.sercomm.openfire.plugin.service.util.ServiceAPIUtil;

@Path(CustomerAPI.URI_PATH)
public class CustomerAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(CustomerAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "admin/";    
    
    @GET
    @Path("customers")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCustomers()
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            List<com.sercomm.openfire.plugin.service.dto.admin.Customer> entity = 
                    new ArrayList<com.sercomm.openfire.plugin.service.dto.admin.Customer>();

            List<EndUserCache> endUsers = EndUserManager.getInstance().getUsers();            
            for(EndUserCache endUser : endUsers)
            {
                com.sercomm.openfire.plugin.service.dto.admin.Customer model = 
                        ServiceAPIUtil.Admin.convert(endUser);
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
    @Path("customers/{userId}")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCustomer(
            @PathParam("userId") String userId)
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            if(XStringUtil.isBlank(userId))
            {
                throw new DemeterException("ARGUMENT(S) CANNOT BE BLANK");
            }
            
            EndUserCache endUser = EndUserManager.getInstance().getUser(userId);
            if(null == endUser)
            {
                throw new DemeterException("USER NOT FOUND: " + userId);
            }
            
            response = Response.status(Status.OK)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Json.build(ServiceAPIUtil.Admin.convert(endUser)))
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

    @PUT
    @Path("customers/{userId}")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response setCustomer(
            @PathParam("userId") String userId,
            String payloadString)
    {
        Response response = null;
        
        String name = XStringUtil.BLANK;
        String password = XStringUtil.BLANK;
        String serial = XStringUtil.BLANK;
        Boolean admin = false;
        Boolean enable = true;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            try
            {
                JsonNode rootNode = Json.parse(payloadString);
                JsonNode nodeName = rootNode.get("name");
                JsonNode nodePassword = rootNode.get("password");
                JsonNode nodeSerial = rootNode.get("serial");
                JsonNode nodeAdmin = rootNode.get("admin");
                JsonNode nodeEnable = rootNode.get("enable");
                
                name = nodeName.asText(XStringUtil.BLANK);
                password = nodePassword.asText(XStringUtil.BLANK);
                serial = nodeSerial.asText(XStringUtil.BLANK);
                admin = nodeAdmin.asBoolean(false);
                enable = nodeEnable.asBoolean(true);
            }
            catch(Throwable ignored)
            {
                throw new DemeterException("INVALID ARGUMENT FORMAT");
            }
            
            if(XStringUtil.isBlank(userId))
            {
                throw new DemeterException("ARGUMENT(S) CANNOT BE BLANK");
            }
            
            EndUserCache endUser = null;
            
            Lock locker = EndUserManager.getInstance().getLock(userId);
            try
            {
                // update password at first if necessary
                String oldPassword = EndUserManager.getInstance().getPassword(userId);
                if(0 != oldPassword.compareTo(password))
                {
                    EndUserManager.getInstance().setPassword(userId, password);
                }

                endUser = EndUserManager.getInstance().getUser(userId);
                if(null == endUser)
                {
                    throw new DemeterException("USER NOT FOUND: " + userId);
                }
                
                // update the other information
                endUser.setEndUserRole(true == admin ? EndUserRole.ADMIN : EndUserRole.MEMBER);
                endUser.setValid(true == enable ? 1 : 0);
                
                endUser.flush();
            }
            finally
            {
                locker.unlock();
            }
            
            response = Response.status(Status.OK)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Json.build(ServiceAPIUtil.Admin.convert(endUser)))
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
        
        log.info("({},{},{},{},{})={}",
            name,
            password,
            serial,
            admin,
            enable,
            errorMessage);

        return response;
    }

    @DELETE
    @Path("customers/{userId}")
    @RequiresRoles({EndUserRole.ADMIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteCustomer(
            @PathParam("userId") String userId)
    {
        Response response = createError(
            Status.NOT_IMPLEMENTED,
            "ERROR",
            "NOT IMPLEMENTED");
        return response;
    }
}
