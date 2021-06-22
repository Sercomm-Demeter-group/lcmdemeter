package com.sercomm.openfire.plugin.c2c.api.v1.device;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.HeaderField;
import com.sercomm.commons.umei.Meta;
import com.sercomm.commons.util.DateTime;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.demeter.microservices.client.v1.GetDevicesResult;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.HttpServer;
import com.sercomm.openfire.plugin.c2c.exception.InternalErrorException;
import com.sercomm.openfire.plugin.c2c.exception.UMEiException;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.define.DeviceState;
import com.sercomm.openfire.plugin.prop.DeviceProperty;

@Path("umei/v1")
public class GetDevicesAPI
{
    private static final Logger log = LoggerFactory.getLogger(GetDevicesAPI.class);

    @GET
    @Path("devices")
    @Produces({MediaType.APPLICATION_JSON})
    public Response execute(
            @HeaderParam(HeaderField.HEADER_REQUEST_ID) String requestId,
            @HeaderParam(HeaderField.HEADER_ORIGINATOR_ID) String originatorId,
            @QueryParam("models") String models,
            @QueryParam("states") String states,
            @QueryParam("from") Integer from,
            @QueryParam("size") Integer size,
            @QueryParam("sort") String sort) 
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        String errorMessage = XStringUtil.BLANK;
        try
        {
            int fromValue = 0;
            int sizeValue = 100;
            if(null != from)
            {
                fromValue = from;
            }
            
            if(null != size)
            {
                if(size > 500)
                {
                    status = Response.Status.FORBIDDEN;
                    errorMessage = "'size' VALUE CANNOT BE LARGER THAN 500";

                    throw new UMEiException(
                        errorMessage,
                        status);
                }
                
                sizeValue = size;
            }
            
            String statesString = XStringUtil.BLANK;
            if(XStringUtil.isBlank(states))
            {
                for(DeviceState deviceState : DeviceState.values())
                {
                    statesString += deviceState.name().toLowerCase();
                    statesString += ",";
                }
            }
            else
            {
                statesString = states;
            }

            boolean hasModels = false;           
            if(XStringUtil.isNotBlank(models))
            {
                hasModels = true;
            }

            // start building the statement
            StringBuilder builder = new StringBuilder();
            builder.append("SELECT SQL_CALC_FOUND_ROWS DISTINCT `serial`,`mac` FROM ");
            builder.append("`sDeviceProp` ");
           
            builder.append("WHERE ");
            
            // node name must be in `ofUser`.`username`
            builder.append("LOWER(CONCAT(`sDeviceProp`.`serial`,'-',`sDeviceProp`.`mac`)) IN (")
                .append("SELECT `username` FROM `ofUser` WHERE `username` = LOWER(CONCAT(`sDeviceProp`.`serial`,'-',`sDeviceProp`.`mac`))) ");
            
            builder.append("AND ");
            
            // states
            builder.append("(`serial`,`mac`) IN (")
                   .append(String.format("SELECT `serial`,`mac` FROM `sDeviceProp` WHERE `name` = '%s' AND `propValue` IN(", DeviceProperty.SERCOMM_DEVICE_STATE.toString()));
        
            Iterator<String> iterator = Arrays.asList(statesString.split(",")).iterator();
            boolean hasNext = false;
            do
            {
                String value = iterator.next();
                DeviceState deviceState = DeviceState.fromString(value.toUpperCase());
                if(null != deviceState)
                {
                    builder.append("'").append(deviceState.name()).append("'");
                }
                
                hasNext = iterator.hasNext();
                if(hasNext)
                {
                    builder.append(",");
                }                            
            }
            while(hasNext);
            
            builder.append(")) ");

            if(hasModels)
            {
                builder.append("AND (`serial`,`mac`) IN (")
                       .append(String.format("SELECT `serial`,`mac` FROM `sDeviceProp` WHERE `name` = '%s' AND `propValue` IN(", DeviceProperty.SERCOMM_DEVICE_MODEL_NAME.toString()));
            
                iterator = Arrays.asList(models.split(",")).iterator();
                do
                {
                    String value = iterator.next();
                    builder.append("'").append(value).append("'");
                    
                    hasNext = iterator.hasNext();
                    if(hasNext)
                    {
                        builder.append(",");
                    }                            
                }
                while(hasNext);
                
                builder.append(")) ");
            }
            
            if(XStringUtil.isBlank(sort))
            {
                sort = "serial:asc";
            }
            
            String order;
            String[] sortTokens = sort.split(":");
            if(2 == sortTokens.length)
            {
                order = sortTokens[1];
            }
            else
            {
                order = "asc";
            }
            
            String orderByColumn = sortTokens[0];
            
            builder.append("ORDER BY ? ? ");
            builder.append("LIMIT ").append(fromValue).append(",").append(sizeValue);
            
            //System.out.println(builder.toString());
            List<String> devices = new ArrayList<>();
            
            int totalCount = 0;
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try
            {
                conn = DbConnectionManager.getConnection();
                
                stmt = conn.prepareStatement(builder.toString());
                
                int idx = 0;
                stmt.setString(++idx, orderByColumn);
                stmt.setString(++idx, order);
                
                rs = stmt.executeQuery();
                
                while(rs.next())
                {
                    String serial = rs.getString("serial");
                    String mac = rs.getString("mac");
                    
                    String nodeName = NameRule.formatDeviceName(serial, mac);
                    devices.add(nodeName);
                }
            }
            finally
            {
                DbConnectionManager.closeStatement(rs, stmt);
                try
                {
                    stmt = conn.prepareStatement("SELECT FOUND_ROWS()");
                    rs = stmt.executeQuery();
                    
                    if(rs.next())
                    {
                        totalCount = rs.getInt(1);
                    }                    
                }
                finally
                {
                    DbConnectionManager.closeConnection(rs, stmt, conn);
                }
            }
            
            List<GetDevicesResult.ResultData> data = new ArrayList<>();
            for(String nodeName : devices)
            {
                DeviceCache deviceCache = DeviceManager.getInstance().getDeviceCache(NameRule.toDeviceSerial(nodeName), NameRule.toDeviceMac(nodeName));

                GetDevicesResult.ResultData row = new GetDevicesResult.ResultData();
                row.setSerial(deviceCache.getSerial());
                row.setMac(deviceCache.getMac());
                row.setModel(deviceCache.getModelName());
                row.setState(deviceCache.getDeviceState().toString());
                row.setFirmware(deviceCache.getFirmwareVersion());
                row.setCreationTime(DateTime.from(deviceCache.getCreationTime()).toString(DateTime.FORMAT_ISO));
                
                data.add(row);
            }
            
            BodyPayload bodyPayload = new BodyPayload()
                    .withMeta(new Meta().withFrom(fromValue).withSize(data.size()).withTotal(totalCount))
                    .withData(data);
            
            response = Response
                    .status(status)
                    .header(HeaderField.HEADER_REQUEST_ID, requestId)
                    .header(HeaderField.HEADER_ORIGINATOR_ID, originatorId)
                    .header(HeaderField.HEADER_RECEIVER_ID, HttpServer.SERVICE_NAME)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(bodyPayload.toString())
                    .build();
        }
        catch(UMEiException e)
        {
            status = e.getErrorStatus();
            errorMessage = e.getMessage();
            throw e.withIdentifyValues(
                requestId,
                originatorId,
                HttpServer.SERVICE_NAME);
        }
        catch(Throwable t)
        {
            status = Response.Status.INTERNAL_SERVER_ERROR;            
            errorMessage = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t);
            throw new InternalErrorException(t.getMessage()).withIdentifyValues(
                requestId,
                HttpServer.SERVICE_NAME, 
                originatorId);
        }

        log.info("({},{},[{}],[{}],{},{},{}); {}",
            requestId,
            originatorId,
            models,
            states,
            from,
            size,
            sort,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }
}
