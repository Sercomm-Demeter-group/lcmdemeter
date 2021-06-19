package com.sercomm.openfire.plugin.service.api.v2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.umei.Meta;
import com.sercomm.commons.util.HttpUtil;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;
import com.sercomm.openfire.plugin.service.util.StringStreamingOutput;

@Path(InstallationsAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR, EndUserRole.OPERATOR})
public class InstallationsAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(InstallationsAPI.class);
    
    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    @Context 
    private HttpServletRequest request;

    @GET
    @Path("installations")
    @Produces({MediaType.APPLICATION_JSON})
    public Response get(
            @QueryParam("from") Integer from,
            @QueryParam("size") Integer size,
            @QueryParam("applicationId") String applicationId,
            @QueryParam("versionId") String versionId,
            @QueryParam("status") String statusValue,
            @QueryParam("filter") List<String> filters)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String errorMessage = XStringUtil.BLANK;
        try
        {
            if(XStringUtil.isBlank(applicationId) || XStringUtil.isBlank(versionId) || XStringUtil.isBlank(statusValue))
            {
                status = Response.Status.BAD_REQUEST;            
                errorMessage = "MANDATORY PARAMETER(S) WAS BLANK";

                throw new UMEiException(errorMessage, status);
            }

            InstallStatus installStatus = InstallStatus.fromString(statusValue);
            if(null == installStatus)
            {
                status = Response.Status.BAD_REQUEST;            
                errorMessage = "INVALID `status` PARAMETER";

                throw new UMEiException(errorMessage, status);
            }

            int totalCount = 0;

            // query total rows count
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try
            {
                List<Object> arguments = new ArrayList<>();

                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(
                    generateQueryTotalStatement(
                        applicationId,
                        versionId,
                        installStatus,
                        filters,
                        arguments));
                
                for(int idx = 0; idx < arguments.size(); idx++)
                {
                    stmt.setObject(idx + 1, arguments.get(idx));
                }

                rs = stmt.executeQuery();
                rs.next();

                totalCount = rs.getInt("count");
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, stmt, conn);
            }

            // query rows data
            List<GetInstallationResult> result = new ArrayList<>();
            try
            {
                List<Object> arguments = new ArrayList<>();

                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(
                    generateQueryRowsStatement(
                        from, 
                        size,
                        applicationId,
                        versionId,
                        installStatus,
                        filters,
                        arguments));
                
                for(int idx = 0; idx < arguments.size(); idx++)
                {
                    stmt.setObject(idx + 1, arguments.get(idx));
                }

                rs = stmt.executeQuery();
                while(rs.next())
                {
                    String serial = rs.getString("serial");
                    String mac = rs.getString("mac");

                    DeviceCache deviceCache =
                    DeviceManager.getInstance().getDeviceCache(
                        NameRule.formatDeviceName(serial, mac));

                    GetInstallationResult object = new GetInstallationResult();
                    object.setSerial(serial);
                    object.setMac(mac);
                    object.setModel(deviceCache.getModelName());
                    object.setFirmware(deviceCache.getFirmwareVersion());

                    result.add(object);
                }
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, stmt, conn);
            }

            // response
            BodyPayload bodyPayload = new BodyPayload()
                    .withMeta(new Meta()
                        .withFrom(from == null ? 0 : from)
                        .withSize(result.size())
                        .withTotal(totalCount))
                    .withData(result);

            response = Response
                .status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(bodyPayload.toString())
                .build();            
        }
        catch(UMEiException e)
        {
            status = e.getErrorStatus();            
            errorMessage = e.getMessage();
            throw e;
        }
        catch(Throwable t)
        {
            status = Response.Status.INTERNAL_SERVER_ERROR;            
            errorMessage = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t);
            throw new InternalErrorException(t.getMessage());
        }

        log.info("({},{},{},{},{},{},{},{}); {}",
            userId,
            sessionId,
            from,
            size,
            applicationId,
            versionId,
            statusValue,
            filters,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    @GET
    @Path("installations/export")
    @Produces({MediaType.APPLICATION_JSON})
    public Response export(
        @QueryParam("applicationId") String applicationId,
        @QueryParam("versionId") String versionId,
        @QueryParam("status") String statusValue,
        @QueryParam("filter") List<String> filters)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;
        
        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        final String[] FILE_HEADER = {"Serial", "MAC"};
        final String FILE_NAME = "export.csv";

        final CSVFormat formatter = CSVFormat.DEFAULT.withHeader(FILE_HEADER).withSkipHeaderRecord();
        final StringBuilder output = new StringBuilder();

        String errorMessage = XStringUtil.BLANK;
        try
        {
            // query rows data
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try
            {
                if(XStringUtil.isBlank(applicationId) || XStringUtil.isBlank(versionId) || XStringUtil.isBlank(statusValue))
                {
                    status = Response.Status.BAD_REQUEST;            
                    errorMessage = "MANDATORY PARAMETER(S) WAS BLANK";
    
                    throw new UMEiException(errorMessage, status);
                }
    
                InstallStatus installStatus = InstallStatus.fromString(statusValue);
                if(null == installStatus)
                {
                    status = Response.Status.BAD_REQUEST;            
                    errorMessage = "INVALID `status` PARAMETER";
    
                    throw new UMEiException(errorMessage, status);
                }
    
                List<Object> arguments = new ArrayList<>();

                conn = DbConnectionManager.getConnection();
                stmt = conn.prepareStatement(
                    generateQueryRowsStatement(
                        null,
                        null,
                        applicationId,
                        versionId,
                        installStatus,
                        filters,
                        arguments));
                
                for(int idx = 0; idx < arguments.size(); idx++)
                {
                    stmt.setObject(idx + 1, arguments.get(idx));
                }

                rs = stmt.executeQuery();
                
                try(CSVPrinter printer = new CSVPrinter(output, formatter))
                {
                    while(rs.next())
                    {
                        List<String> records = new ArrayList<>();
                        records.add(rs.getString("serial"));
                        records.add(rs.getString("mac"));
                        
                        printer.printRecord(records);
                    }
                }
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, stmt, conn);
            }

            response = Response.ok(new StringStreamingOutput(output.toString()))
                    .type(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpUtil.HEADER_CONTEXT_DISPOS, String.format("attachment; filename=\"%s\"", FILE_NAME))
                    .build();
        }
        catch(UMEiException e)
        {
            status = e.getErrorStatus();            
            errorMessage = e.getMessage();
            throw e;
        }
        catch(Throwable t)
        {
            status = Response.Status.INTERNAL_SERVER_ERROR;            
            errorMessage = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t);
            throw new InternalErrorException(t.getMessage());
        }

        log.info("({},{},{}); {}",
            userId,
            sessionId,
            filters,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }
    
    private static String generateQueryTotalStatement(
        String applicationId,
        String versionId,
        InstallStatus installStatus,
        List<String> filters,
        List<Object> arguments)
    throws UMEiException, InternalErrorException
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT COUNT(*) AS `count` FROM (");
        builder.append("SELECT `serial`,`mac` FROM `sDeviceProp` AS `t1` WHERE ");
        
        switch(installStatus)
        {
            case INSTALLED:
                builder.append("EXISTS ");
                break;
            case NOT_INSTALLED:
                builder.append("NOT EXISTS ");
                break;
        }

        builder.append("(SELECT * FROM `sAppInstallation` WHERE `serial`=`t1`.serial AND `mac`=`t1`.`mac` AND `appId`=? AND `versionId`=?) ");
        arguments.add(applicationId);
        arguments.add(versionId);

        List<String[]> idFilters = new ArrayList<>();
        List<String[]> propFilters = new ArrayList<>();

        if(!filters.isEmpty())
        {
            builder.append("AND ");

            // preprocess the filters
            Iterator<String> iterator = filters.iterator();
            while(iterator.hasNext())
            {
                final String filter = iterator.next();
                String[] tokens = filter.split(":", 2);
                if(2 != tokens.length)
                {
                    throw new UMEiException(
                        "INVALID FILTER: " + filter,
                        Response.Status.BAD_REQUEST);
                }

                switch(tokens[0])
                {
                    case "serial":
                        idFilters.add(tokens);
                        break;
                    case "mac":
                        idFilters.add(tokens);
                        break;
                    case "model":
                        propFilters.add(tokens);
                        break;
                    case "firmware":
                        propFilters.add(tokens);
                        break;
                    default:
                        throw new UMEiException(
                            "INVALID FILTER ATTRIBUTE: " + tokens[0],
                            Response.Status.BAD_REQUEST);
                }
            }                
        }

        // filter serial and mac
        Iterator<String[]> iterator = idFilters.iterator();
        while(iterator.hasNext())
        {
            final String[] tokens = iterator.next();

            switch(tokens[0])
            {
                case "serial":
                    builder.append("`t1`.`serial` LIKE ? ");
                    arguments.add(tokens[1] + "%");
                    break;
                case "mac":
                    builder.append("`t1`.`mac` LIKE ? ");
                    arguments.add(tokens[1] + "%");
                    break;
            }

            if(iterator.hasNext())
            {
                builder.append("AND ");
            }
            else
            {
                if(!propFilters.isEmpty())
                {
                    builder.append("AND ");
                }
            }
        }

        int filterPropCount = 0;

        // filter properties
        iterator = propFilters.iterator();
        while(iterator.hasNext())
        {
            final String[] tokens = iterator.next();

            switch(tokens[0])
            {
                case "model":
                    builder.append("(`name`='sercomm.device.model.name' AND `propValue` LIKE ?) ");
                    arguments.add(tokens[1] + "%");
                    filterPropCount++;
                    break;
                case "firmware":
                    builder.append("(`name`='sercomm.device.firmware.version' AND `propValue` LIKE ?) ");
                    arguments.add(tokens[1] + "%");
                    filterPropCount++;
                    break;
            }
            
            if(iterator.hasNext())
            {
                builder.append("OR ");
            }
        }
        
        builder.append("GROUP BY `t1`.`serial`,`t1`.`mac` ");
        builder.append("HAVING COUNT(`t1`.`serial`) > ? AND COUNT(`t1`.`mac`) > ?");
        arguments.add(filterPropCount == 0 ? 0 : filterPropCount - 1);
        arguments.add(filterPropCount == 0 ? 0 : filterPropCount - 1);
        
        builder.append(") AS `t`");
        
        return builder.toString();
    }

    private static String generateQueryRowsStatement(
        Integer from,
        Integer size,
        String applicationId,
        String versionId,
        InstallStatus installStatus,
        List<String> filters,
        List<Object> arguments)
    throws UMEiException, InternalErrorException
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT `serial`,`mac` FROM `sDeviceProp` AS `t1` WHERE ");

        switch(installStatus)
        {
            case INSTALLED:
                builder.append("EXISTS ");
                break;
            case NOT_INSTALLED:
                builder.append("NOT EXISTS ");
                break;
        }

        builder.append("(SELECT * FROM `sAppInstallation` WHERE `serial`=`t1`.serial AND `mac`=`t1`.`mac` AND `appId`=? AND `versionId`=?) ");
        arguments.add(applicationId);
        arguments.add(versionId);

        List<String[]> idFilters = new ArrayList<>();
        List<String[]> propFilters = new ArrayList<>();

        if(!filters.isEmpty())
        {
            builder.append("AND ");

            // preprocess the filters
            Iterator<String> iterator = filters.iterator();
            while(iterator.hasNext())
            {
                final String filter = iterator.next();
                String[] tokens = filter.split(":", 2);
                if(2 != tokens.length)
                {
                    throw new UMEiException(
                        "INVALID FILTER: " + filter,
                        Response.Status.BAD_REQUEST);
                }

                switch(tokens[0])
                {
                    case "serial":
                        idFilters.add(tokens);
                        break;
                    case "mac":
                        idFilters.add(tokens);
                        break;
                    case "model":
                        propFilters.add(tokens);
                        break;
                    case "firmware":
                        propFilters.add(tokens);
                        break;
                    default:
                        throw new UMEiException(
                            "INVALID FILTER ATTRIBUTE: " + tokens[0],
                            Response.Status.BAD_REQUEST);
                }
            }                
        }

        // filter serial and mac
        Iterator<String[]> iterator = idFilters.iterator();
        while(iterator.hasNext())
        {
            final String[] tokens = iterator.next();

            switch(tokens[0])
            {
                case "serial":
                    builder.append("`t1`.`serial` LIKE ? ");
                    arguments.add(tokens[1] + "%");
                    break;
                case "mac":
                    builder.append("`t1`.`mac` LIKE ? ");
                    arguments.add(tokens[1] + "%");
                    break;
            }

            if(iterator.hasNext())
            {
                builder.append("AND ");
            }
            else
            {
                if(!propFilters.isEmpty())
                {
                    builder.append("AND ");
                }
            }
        }

        int filterPropCount = 0;

        // filter properties
        iterator = propFilters.iterator();
        while(iterator.hasNext())
        {
            final String[] tokens = iterator.next();

            switch(tokens[0])
            {
                case "model":
                    builder.append("(`name`='sercomm.device.model.name' AND `propValue` LIKE ?) ");
                    arguments.add(tokens[1]);
                    filterPropCount++;
                    break;
                case "firmware":
                    builder.append("(`name`='sercomm.device.firmware.version' AND `propValue` LIKE ?) ");
                    arguments.add(tokens[1]);
                    filterPropCount++;
                    break;
            }
            
            if(iterator.hasNext())
            {
                builder.append("OR ");
            }
        }
        
        builder.append("GROUP BY `t1`.`serial`,`t1`.`mac` ");
        builder.append("HAVING COUNT(`t1`.`serial`) > ? AND COUNT(`t1`.`mac`) > ? ");
        arguments.add(filterPropCount == 0 ? 0 : filterPropCount - 1);
        arguments.add(filterPropCount == 0 ? 0 : filterPropCount - 1);
        
        if(null != from && null != size)
        {
            builder.append("LIMIT ?,?");
            arguments.add(from);
            arguments.add(size);
        }
        
        return builder.toString();
    }

    public enum InstallStatus
    {
        INSTALLED(1),
        NOT_INSTALLED(2);

        private static Map<String, InstallStatus> map =
            new ConcurrentHashMap<>();
        static
        {
            for(InstallStatus installStatus : InstallStatus.values())
            {
                map.put(installStatus.toString(), installStatus);
            }
        }

        private Integer value;
        private InstallStatus(Integer value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return Integer.toString(this.value);
        }

        public int intValue()
        {
            return this.value;
        }

        public static InstallStatus fromString(String value)
        {
            return map.get(value);
        }
    }

    public static class GetInstallationResult
    {
        private String serial;
        private String mac;
        private String model;
        private String firmware;

        public String getSerial()
        {
            return serial;
        }
        public void setSerial(String serial)
        {
            this.serial = serial;
        }
        public String getMac()
        {
            return mac;
        }
        public void setMac(String mac)
        {
            this.mac = mac;
        }
        public String getModel()
        {
            return model;
        }
        public void setModel(String model)
        {
            this.model = model;
        }
        public String getFirmware()
        {
            return firmware;
        }
        public void setFirmware(String firmware)
        {
            this.firmware = firmware;
        }
    }
}
