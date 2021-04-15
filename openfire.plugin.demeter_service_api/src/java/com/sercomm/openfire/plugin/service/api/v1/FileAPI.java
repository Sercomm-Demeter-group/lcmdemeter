package com.sercomm.openfire.plugin.service.api.v1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.util.HttpUtil;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.SystemProperties;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.data.frontend.AppIcon;
import com.sercomm.openfire.plugin.data.frontend.AppVersion;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.profile.IProfile;
import com.sercomm.openfire.plugin.profile.Profile;
import com.sercomm.openfire.plugin.service.api.ServiceAPIBase;
import com.sercomm.openfire.plugin.util.IpkUtil;
import com.sercomm.openfire.plugin.util.StorageUtil;

@Path(FileAPI.URI_PATH)
public class FileAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(FileAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v1/";    
    
    private static final int BUFFER_LENGTH = 256;
    
    @GET
    @Path("files/icon/{iconId}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getIcon(
            @PathParam("iconId") String iconId)
    {
        Response response = null;
        
        String errorMessage = XStringUtil.BLANK;        
        try
        {
            AppIcon appIcon = AppManager.getInstance().getAppIcon(iconId);
            do
            {
                if(null == appIcon)
                {
                    response = Response.status(Status.NO_CONTENT).build();
                    break;
                }
                
                response = Response.status(Status.OK)
                        .type(MediaType.APPLICATION_OCTET_STREAM)
                        .entity(appIcon.getData())
                        .build();
            }
            while(false);
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
    
    @GET
    @Path("files/package/{versionId}/{filename}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getPackageFile(
            @PathParam("versionId") String versionId,
            @PathParam("filename") String filename)
    {
        Response response = null;

        String errorMessage = XStringUtil.BLANK;
        try
        {
            if(XStringUtil.isBlank(versionId) ||
               XStringUtil.isBlank(filename))
            {
                throw new DemeterException("ARGUMENT(S) CANNOT BE BLANK");
            }
            
            do
            {
                AppVersion appVersion = AppManager.getInstance().getAppVersion(versionId);
                if(null == appVersion)
                {
                    response = Response.noContent().build();
                    break;
                }
                
                if(filename.endsWith("gz"))
                {
                    // package info. file
                    App app = AppManager.getInstance().getApp(appVersion.getAppId());
                    String folderPathString = StorageUtil.Path.makePackageFolderPath(
                        SystemProperties.getInstance().getStorage().getRootPath(), 
                        app.getId(), 
                        versionId);

                    String packageInfoFilePathString = folderPathString + File.separator + IpkUtil.PACKAGE_GZ_FILENAME;
                    java.nio.file.Path packageInfoFilePath = Paths.get(packageInfoFilePathString);
                    if(false == Files.exists(packageInfoFilePath))
                    {
                        response = Response.noContent().build();
                        break;
                    }
                    
                    response = Response.ok(new FileStreamingOutput(packageInfoFilePath))
                            .header(HttpUtil.HEADER_CONTEXT_DISPOS, String.format("attachment; filename=\"%s\"", IpkUtil.PACKAGE_GZ_FILENAME))
                            .build();
                }
                else if(filename.endsWith("ipk"))
                {
                    // IPK file
                    String ipkFilePathString = appVersion.getIPKFilePath();
                    java.nio.file.Path ipkFilePath = Paths.get(ipkFilePathString);
                    if(false == Files.exists(ipkFilePath))
                    {
                        response = Response.noContent().build();
                        break;
                    }
                    
                    response = Response.ok(new FileStreamingOutput(ipkFilePath))
                            .header(HttpUtil.HEADER_CONTEXT_DISPOS, String.format("attachment; filename=\"%s\"", filename))
                            .build();
                }
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
        
        log.info("({},{})={}",
            versionId,
            filename,
            errorMessage);
        
        return response;
    }

    @GET
    @Path("files/lxc_conf/{deviceId}/{filename}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getContainerConfiguration(
            @PathParam("deviceId") String deviceId,
            @PathParam("filename") String filename)
    {
        Response response = null;

        String serial = XStringUtil.BLANK;
        String mac = XStringUtil.BLANK;
        String appName = XStringUtil.BLANK;
        
        String errorMessage = XStringUtil.BLANK;
        try
        {
            if(XStringUtil.isBlank(deviceId) ||
               XStringUtil.isBlank(filename))
            {
                throw new DemeterException("ARGUMENT(S) CANNOT BE BLANK");
            }
            
            try
            {
                serial = NameRule.toDeviceSerial(deviceId);
                mac = NameRule.toDeviceMac(deviceId);
            }
            catch(Throwable ignored)
            {
                errorMessage = "INVALID ARGUMENT FORMAT";
                throw new DemeterException(errorMessage);
            }

            appName = FilenameUtils.getBaseName(filename);
            
            String modelName;
            try
            {
                DeviceCache deviceCache = DeviceManager.getInstance().getDeviceCache(serial, mac);
                modelName = deviceCache.getModelName();
            }
            catch(Throwable ignored)
            {
                errorMessage = "INVALID S/N OR MAC";
                throw new DemeterException(errorMessage);
            }

            IProfile profile = Profile.get(modelName);
            String configurationXML = profile.generateContainerConfiguration(serial, mac, appName);
        
            response = Response.ok(configurationXML)
                    .header(HttpUtil.HEADER_CONTEXT_DISPOS, String.format("attachment; filename=\"%s\"", filename))
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
            deviceId,
            filename,
            errorMessage);
        
        return response;
    }

    public static class FileStreamingOutput implements StreamingOutput 
    {
        private final java.nio.file.Path ipkFilePath;
        
        public FileStreamingOutput(java.nio.file.Path ipkFilePath)
        {
            this.ipkFilePath = ipkFilePath;
        }
        
        @Override
        public void write(OutputStream output)
        throws IOException, WebApplicationException 
        {
            try(FileInputStream fis = new FileInputStream(this.ipkFilePath.toFile()))
            {
                byte[] buffer = new byte[BUFFER_LENGTH];
                int length = 0;
                
                while((length = fis.read(buffer, 0, BUFFER_LENGTH)) != -1) 
                {
                    output.write(buffer, 0, length);
                }
                output.flush();
            } 
            catch(Throwable t) 
            {  
                log.error(t.getMessage(), t); 
            }
        }
    }
}
