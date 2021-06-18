package com.sercomm.openfire.plugin.service.api.v2;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.umei.BodyPayload;
import com.sercomm.commons.util.HttpUtil;
import com.sercomm.commons.util.Json;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.BatchManager;
import com.sercomm.openfire.plugin.data.frontend.Batch;
import com.sercomm.openfire.plugin.data.frontend.BatchData;
import com.sercomm.openfire.plugin.define.BatchCommand;
import com.sercomm.openfire.plugin.define.BatchState;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.service.annotation.RequireRoles;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;
import com.sercomm.openfire.plugin.service.util.StringStreamingOutput;

@Path(BatchAPI.URI_PATH)
@RequireRoles({EndUserRole.ADMIN, EndUserRole.EDITOR, EndUserRole.OPERATOR})
public class BatchAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(BatchAPI.class);
    
    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v2/";

    @Context 
    private HttpServletRequest request;

    @POST
    @Path("batch")
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON})
    public Response post(
        @FormDataParam("payload") String requestPayload,
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition fdcd)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String applicationId = XStringUtil.BLANK;
        String versionId = XStringUtil.BLANK;
        Integer command = null;
        int size = 0;

        String errorMessage = XStringUtil.BLANK;
        try
        {
            BatchCommand batchCommand = null;

            if(XStringUtil.isBlank(requestPayload))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "REQUEST PAYLOAD WAS BLANK";

                throw new UMEiException(
                    errorMessage,
                    Response.Status.BAD_REQUEST);
            }

            try
            {
                BodyPayload bodyPayload = Json.mapper().readValue(
                    requestPayload,
                    BodyPayload.class);
    
                PostBatchRequest request = bodyPayload.getDesire(
                    PostBatchRequest.class);
                
                applicationId = request.getApplicationId();
                versionId = request.getVersionId();
                command = request.getCommand();
            }
            catch(Throwable ignored)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID REQUEST PAYLOAD: " + requestPayload;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            batchCommand = BatchCommand.fromValue(command);
            if(null == batchCommand)
            {
                throw new UMEiException(
                    "INVALID 'command' value",
                    Response.Status.BAD_REQUEST);
            }

            if(null == fileInputStream)
            {
                throw new UMEiException(
                    "NO INPUT FILE",
                    Response.Status.BAD_REQUEST);
            }

            if(0 != "csv".compareToIgnoreCase(FilenameUtils.getExtension(fdcd.getFileName())))
            {
                throw new UMEiException(
                    "INVALID INPUT FILE EXTENSION",
                    Response.Status.BAD_REQUEST);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int length = 0;
            byte[] buffer = new byte[1024];
            while ((length = fileInputStream.read(buffer, 0, buffer.length)) != -1)
            {
                baos.write(buffer, 0, length);
            }
            baos.flush();

            // obtain the file data
            byte[] bufferArray = baos.toByteArray();
            // obtain the file size
            size = bufferArray.length;

            String csvText = new String(bufferArray);

            List<String> totalDevices = new ArrayList<>();
            try(Reader reader = new StringReader(csvText))
            {
                try(CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT))
                {
                    for(CSVRecord csvRecord : csvParser)
                    {
                        String serial = csvRecord.get(0);
                        String mac = csvRecord.get(1);

                        totalDevices.add(NameRule.formatDeviceName(serial, mac));
                    }
                }
                catch(IllegalArgumentException e)
                {
                    throw new UMEiException(
                        "INVALID INPUT FILE: " + e.getMessage(), 
                        Status.BAD_REQUEST);
                }
            }

            if(0 == totalDevices.size())
            {
                throw new UMEiException(
                    "INPUT FILE IS BLANK", 
                    Status.FORBIDDEN);
            }

            try
            {
                BatchManager.getInstance().updateBatch(
                    XStringUtil.BLANK, 
                    applicationId, 
                    versionId, 
                    batchCommand,
                    BatchState.PENDING,
                    totalDevices, 
                    new ArrayList<String>(),
                    new ArrayList<String>());
            }
            catch(DemeterException e)
            {
                errorMessage = e.getMessage();
                status = Status.FORBIDDEN;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            // response
            BodyPayload bodyPayload = new BodyPayload()
            .withMeta(null)
            .withData(null);
    
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

        log.info("({},{},{},{},{},{}); {}",
            userId,
            sessionId,
            applicationId,
            versionId,
            command,
            size,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    @PUT
    @Path("batch/{batchId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response put(
            @PathParam("batchId") String batchId,
            String requestPayload)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String statusString = XStringUtil.BLANK;

        String errorMessage = XStringUtil.BLANK;
        try
        {
            Batch batch = null;
            try
            {
                batch = BatchManager.getInstance().getBatch(batchId);
            }
            catch(DemeterException e)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = e.getMessage();

                throw new UMEiException(
                    errorMessage,
                    Response.Status.BAD_REQUEST);
            }

            if(XStringUtil.isBlank(requestPayload))
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "REQUEST PAYLOAD WAS BLANK";

                throw new UMEiException(
                    errorMessage,
                    Response.Status.BAD_REQUEST);
            }

            try
            {
                BodyPayload bodyPayload = Json.mapper().readValue(
                    requestPayload,
                    BodyPayload.class);
    
                PutBatchRequest request = bodyPayload.getDesire(
                    PutBatchRequest.class);
    
                statusString = request.getStatus();
            }
            catch(Throwable ignored)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = "INALID REQUEST PAYLOAD: " + requestPayload;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            BatchStatus batchStatus = BatchStatus.fromString(statusString);
            if(null == batchStatus)
            {
                status = Response.Status.FORBIDDEN;
                errorMessage = "INALID 'status' VALUE: " + statusString;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            BatchState oldState = BatchState.fromString(batch.getState());

            BatchState newState = null;
            switch(batchStatus)
            {
                case EXECUTING:
                    if(oldState == BatchState.PENDING || oldState == BatchState.PAUSED)
                    {
                        newState = BatchState.PENDING;
                    }
                    else
                    {
                        status = Response.Status.FORBIDDEN;
                        errorMessage = "ILLEGAL DESIRE STATUS: CURRENT STATUS IS " + batch.getState();
        
                        throw new UMEiException(
                            errorMessage,
                            status);
                    }
                    break;
                case PAUSED:
                    if(oldState == BatchState.PENDING || oldState == BatchState.EXECUTING)
                    {
                        newState = BatchState.PAUSING;
                    }
                    else
                    {
                        status = Response.Status.FORBIDDEN;
                        errorMessage = "ILLEGAL DESIRE STATUS: CURRENT STATUS IS " + batch.getState();
        
                        throw new UMEiException(
                            errorMessage,
                            status);
                    }
                    break;
                case TERMINATED:
                    if(oldState == BatchState.PENDING || oldState == BatchState.EXECUTING)
                    {
                        newState = BatchState.TERMINATING;
                    }
                    else
                    {
                        status = Response.Status.FORBIDDEN;
                        errorMessage = "ILLEGAL DESIRE STATUS: CURRENT STATUS IS " + batch.getState();
        
                        throw new UMEiException(
                            errorMessage,
                            status);
                    }
                    break;
            }

            BatchData batchData = new BatchData(batch.getData());
            try
            {
                BatchManager.getInstance().updateBatch(
                    batchId,
                    batch.getApplicationId(),
                    batch.getVersionId(),
                    BatchCommand.fromValue(batch.getCommand()),
                    newState,
                    batchData.getTotalDevices(),
                    batchData.getDoneDevices(),
                    batchData.getFailedDevices());
            }
            catch(DemeterException e)
            {
                errorMessage = e.getMessage();
                status = Status.FORBIDDEN;

                throw new UMEiException(
                    errorMessage,
                    status);
            }

            // response
            BodyPayload bodyPayload = new BodyPayload()
            .withMeta(null)
            .withData(null);
    
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

        log.info("({},{},{},{}); {}",
            userId,
            sessionId,
            batchId,
            statusString,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    @GET
    @Path("batch/{batchId}/export")
    @Produces({MediaType.APPLICATION_JSON})
    public Response export(
            @PathParam("batchId") String batchId)
    throws UMEiException, InternalErrorException
    {
        Response response = null;
        Response.Status status = Status.OK;

        final String userId = (String) request.getAttribute("userId");
        final String sessionId = (String) request.getAttribute("sessionId");

        String errorMessage = XStringUtil.BLANK;
        try
        {
            Batch batch = null;
            try
            {
                batch = BatchManager.getInstance().getBatch(batchId);
            }
            catch(DemeterException e)
            {
                status = Response.Status.BAD_REQUEST;
                errorMessage = e.getMessage();

                throw new UMEiException(
                    errorMessage,
                    Response.Status.BAD_REQUEST);
            }

            BatchState batchState = BatchState.fromString(batch.getState());
            if(batchState != BatchState.TERMINATED && batchState != BatchState.DONE)
            {
                status = Response.Status.FORBIDDEN;
                errorMessage = "BATCH TASK HAS NOT BEEN TERMINATED YET. PLEASE TERMINATE IT AT FIRST.";

                throw new UMEiException(
                    errorMessage,
                    Response.Status.BAD_REQUEST);
            }

            final String[] FILE_HEADER = {"Serial", "MAC"};
            final String FILE_NAME = "export.csv";
    
            final CSVFormat formatter = CSVFormat.DEFAULT.withHeader(FILE_HEADER).withSkipHeaderRecord();
            final StringBuilder output = new StringBuilder();
    
            try(CSVPrinter printer = new CSVPrinter(output, formatter))
            {
                BatchData batchData = new BatchData(batch.getData());
                for(String deviceId : batchData.getFailedDevices())
                {
                    List<String> records = new ArrayList<>();
                    records.add(NameRule.toDeviceSerial(deviceId));
                    records.add(NameRule.toDeviceMac(deviceId));

                    printer.printRecord(records);
                }
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

        log.info("({},{}); {}",
            userId,
            sessionId,
            XStringUtil.isNotBlank(errorMessage) ? status.getStatusCode() + ",errors: " + errorMessage : status.getStatusCode());

        return response;
    }

    private enum BatchStatus
    {
        EXECUTING("executing"),
        PAUSED("paused"),
        TERMINATED("terminated");

        private static Map<String, BatchStatus> map =
            new ConcurrentHashMap<>();
        static
        {
            for(BatchStatus batchStatus : BatchStatus.values())
            {
                map.put(batchStatus.toString(), batchStatus);
            }
        }
        private String value;
        private BatchStatus(String value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return this.value;
        }

        public static BatchStatus fromString(String value)
        {
            return map.get(value);
        }
    }

    public static class PostBatchRequest
    {
        private String applicationId;
        private String versionId;
        private Integer command;

        public String getApplicationId()
        {
            return this.applicationId;
        }

        public void setApplicationId(String applicationId)
        {
            this.applicationId = applicationId;
        }

        public String getVersionId()
        {
            return this.versionId;
        }

        public void setVersionId(String versionId)
        {
            this.versionId = versionId;
        }

        public Integer getCommand()
        {
            return this.command;
        }

        public void setCommand(Integer command)
        {
            this.command = command;
        }
    }

    public static class PutBatchRequest
    {
        private String status;

        public String getStatus()
        {
            return this.status;
        }

        public void setStatus(String status)
        {
            this.status = status;
        }
    }
}
