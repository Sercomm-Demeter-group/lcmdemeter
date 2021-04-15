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
import com.sercomm.openfire.plugin.OwnershipManager;
import com.sercomm.openfire.plugin.ServiceSessionManager;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.cache.OwnershipCache;
import com.sercomm.openfire.plugin.cache.ServiceSessionCache;
import com.sercomm.openfire.plugin.define.HttpHeader;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.profile.IProfile;
import com.sercomm.openfire.plugin.profile.Profile;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;

@Path(UsageAPI.URI_PATH)
public class UsageAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(UsageAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v1/";    

    @GET
    @Path("usage")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUsage(
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
            
            DeviceCache deviceCache = DeviceManager.getInstance().getDeviceCache(serial, mac);
            if(null == deviceCache)
            {
                throw new DemeterException("DEVICE CANNOT BE FOUND");
            }

            final String modelName = deviceCache.getModelName();
            IProfile profile = Profile.get(modelName);
            
            Map<?, ?> dataModel;
            try
            {
                dataModel = profile.getUsage(serial, mac, XStringUtil.BLANK);
            }
            catch(Throwable t1)
            {
                throw new DemeterException("REMOTE GATEWAY WAS NOT AVAILABLE TEMPORARILY: " + t1.getMessage());
            }
            
            UsageModel usageModel = Json.mapper().readValue(Json.build(dataModel.get("Body")), UsageModel.class);
            
            ResponseEntity entity = new ResponseEntity();
            entity.cpu = Double.parseDouble(usageModel.Resources.CPU.Usage.replace("%", ""));
            entity.memory.total = usageModel.Resources.Memory.Total;
            entity.memory.used = usageModel.Resources.Memory.Total - usageModel.Resources.Memory.Free;
            entity.disk.total = usageModel.Resources.Storage.Total;
            entity.disk.used = usageModel.Resources.Storage.Total - usageModel.Resources.Storage.Free;
        
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
        
        return response;
    }
    
    public final static class UsageModel
    {
        public final static class CPU
        {
            public String Usage = "0.0";
        }
        
        public final static class Memory
        {
            public Long Total = 0L;
            public Long Free = 0L;
            public Double Usage = 0.;
        }
        public final static class Storage
        {
            public Long Total = 0L;
            public Long Free = 0L;
            public Double Usage = 0.;
        }
        public final static class Resources
        {
            public CPU CPU;
            public Storage Storage;
            public Memory Memory;
        }
        
        public String Id;
        public String Name;
        public Boolean Enabled;
        public String Version;
        public String Vendor;
        public String Type;
        public String Status;
        public Resources Resources;
    }
    
    public final static class ResponseEntity
    {
        public final static class Disk
        {
            public Long total;
            public Long used;
        }
        
        public final static class Memory
        {
            public Long total;
            public Long used;
        }
        
        public Double cpu;
        public Disk disk = new Disk();
        public Memory memory = new Memory();
    }
}
