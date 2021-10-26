package com.sercomm.openfire.plugin.service.api.v1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;
import java.util.UUID;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
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
import com.sercomm.openfire.plugin.define.StorageType;
import com.sercomm.openfire.plugin.AppManager;

import io.minio.MinioClient;
import io.minio.DownloadObjectArgs;
import io.minio.errors.MinioException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


@Path(FileAPI.URI_PATH)
public class FileAPI extends ServiceAPIBase
{
    private static final Logger log = LoggerFactory.getLogger(FileAPI.class);

    protected static final String URI_PATH = ServiceAPIBase.URI_PATH + "v1/";    
    
    private static final int BUFFER_LENGTH = 256;
    


    private String read_xml_from_file(java.nio.file.Path file_path)
    throws IOException
    {
        String data = null;
        // check if the package manifest exists
        if(true == Files.exists(file_path))
        {
            data = Files.lines(file_path).reduce("", String::concat);
        }
        return data;
    }

    public String getXMLFromFile(String appName)
    throws DemeterException, Throwable
    {
        List<App> apps = AppManager.getInstance().getApps();
        for(App app : apps)
        {
            if(app.getName().equals(appName))
            {
                String FSRootPathString = SystemProperties.getInstance().getStorage().getRootPath();
                if(FSRootPathString.endsWith(File.separator) == false){
                    FSRootPathString = FSRootPathString.concat(File.separator);
                }

                final String manifestFilePathString = app.getId() + File.separator + IpkUtil.PACKAGE_MANIFEST_FILENAME;
                java.nio.file.Path manifestFilePath = Paths.get(FSRootPathString + manifestFilePathString);
                final StorageType storage_type = SystemProperties.getInstance().getStorage().getStorageType();

                if(storage_type == StorageType.LOCAL_FS)
                {
                    try
                    {
                        return read_xml_from_file(manifestFilePath);
                    }
                    catch(IOException e)
                    {
                         throw new IOException("FAILED TO READ MANIFEST FILE: " + e.getMessage());
                    }

                } else {

                    boolean tmp_file_created = false;
                    boolean manifest_downloaded = false;
                    java.nio.file.Path path = null;
                    String xml = null;

                    try{
                        path = Files.createTempFile(null, UUID.randomUUID().toString());
                        tmp_file_created = true;
                        LoadFileFromCloud(path.toAbsolutePath().toString(), manifestFilePathString);
                        manifest_downloaded = true;
                        xml = read_xml_from_file(path);
                    }
                    catch(IOException e){

                        String reason = null;

                        if(tmp_file_created){
                            if(manifest_downloaded){
                                reason = "CANNOT PARSE DOWNLOADED MANIFEST FILE: " +
                                manifestFilePathString +
                                " Message: " +
                                e.getMessage();
                            } else {
                                reason = "CANNOT DOWNLOAD MANIFEST FILE FROM CLOUD: " +
                                manifestFilePathString +
                                " Message: " +
                                e.getMessage();
                            }
                        } else {
                            reason = "CANNOT CREATE TEMPORARY FILE: " +
                            " Message: " +
                            e.getMessage();
                        }

                        throw new DemeterException(reason);
                    }
                    finally{
                        if(tmp_file_created){
                            FileUtils.forceDelete(new File(path.toString()));
                        }
                    }

                    return xml;                        
                    
                }
            }
        }
        return null;
    }

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
        boolean tmp_file_created = false;
        String tmp_file_name = null;


        String errorMessage = XStringUtil.BLANK;
        try
        {
            if(XStringUtil.isBlank(versionId) ||
               XStringUtil.isBlank(filename))
            {
                throw new DemeterException("ARGUMENT(S) CANNOT BE BLANK");
            }

            final StorageType storage_type = SystemProperties.getInstance().getStorage().getStorageType();
            if(! (storage_type == StorageType.LOCAL_FS || storage_type == StorageType.AWS_S3) ){
                throw new DemeterException("STORAGE TYPE IS NOT SUPPORTED");
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
                    java.nio.file.Path packageInfoFilePath;
                    // package info. file
                    App app = AppManager.getInstance().getApp(appVersion.getAppId());

                    if(storage_type == StorageType.LOCAL_FS){

                        String FSRootPathString = SystemProperties.getInstance().getStorage().getRootPath();
                        if(FSRootPathString.endsWith(File.separator) == false){
                            FSRootPathString = FSRootPathString.concat(File.separator);
                        }

                        String packageInfoFilePathString = 
                            FSRootPathString +
                            app.getId() +
                            File.separator +
                            versionId +
                            File.separator +
                            IpkUtil.PACKAGE_GZ_FILENAME;

                        packageInfoFilePath = Paths.get(packageInfoFilePathString);
                        if(false == Files.exists(packageInfoFilePath))
                        {
                            response = Response.noContent().build();
                            break;
                        }

                    } else {

                        final String cloud_obj_name =
                            app.getId() +
                            File.separator +
                            versionId +
                            File.separator +
                            IpkUtil.PACKAGE_GZ_FILENAME;
                        try{
                            java.nio.file.Path path = Files.createTempFile(null, UUID.randomUUID().toString()+".gz");
                            tmp_file_name = path.toAbsolutePath().toString();
                            tmp_file_created = true;
                            LoadFileFromCloud(tmp_file_name, cloud_obj_name);
                        }
                        catch(IOException e){
                            throw new DemeterException("CANNOT CREATE TEMPORARY FILE: " +
                                tmp_file_name +
                                " Message: " +
                                e.getMessage());
                        }
                        
                        packageInfoFilePath = Paths.get(tmp_file_name);
                    }

                    response = Response.ok(new FileStreamingOutput(packageInfoFilePath, tmp_file_created))
                            .header(HttpUtil.HEADER_CONTEXT_DISPOS, String.format("attachment; filename=\"%s\"", IpkUtil.PACKAGE_GZ_FILENAME))
                            .build();

                    tmp_file_created = false;
                }
                else if(filename.endsWith("ipk"))
                {
                    // IPK file
                    java.nio.file.Path ipkFilePath;

                    if(storage_type == StorageType.LOCAL_FS){

                        String FSRootPathString = SystemProperties.getInstance().getStorage().getRootPath();
                        if(FSRootPathString.endsWith(File.separator) == false){
                            FSRootPathString = FSRootPathString.concat(File.separator);
                        }
                        ipkFilePath = Paths.get(FSRootPathString + appVersion.getIPKFilePath());
                        if(false == Files.exists(ipkFilePath))
                        {
                            response = Response.noContent().build();
                            break;
                        }

                    } else {

                        final String cloud_obj_name = appVersion.getIPKFilePath();
                        try{
                            java.nio.file.Path path = Files.createTempFile(null, UUID.randomUUID().toString()+".ipk");
                            tmp_file_name = path.toAbsolutePath().toString();
                            tmp_file_created = true;
                            LoadFileFromCloud(tmp_file_name, cloud_obj_name);
                        }
                        catch(IOException e){
                            throw new DemeterException("CANNOT CREATE TEMPORARY FILE: " +
                                tmp_file_name +
                                " Message: " +
                                e.getMessage());
                        }
                        
                        ipkFilePath = Paths.get(tmp_file_name);

                    }
                    
                    response = Response.ok(new FileStreamingOutput(ipkFilePath, tmp_file_created))
                        .header(HttpUtil.HEADER_CONTEXT_DISPOS, String.format("attachment; filename=\"%s\"", filename))
                        .build();
                    
                    tmp_file_created = false;
                }
            }
            while(false);
        }
        catch(DemeterException e)
        {
            errorMessage = e.getMessage();
            Status status = e.GetHttpStatus();
            response = createError(
                status != null ? status : Status.FORBIDDEN,
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
        finally{
            if(tmp_file_created && tmp_file_name != null){
                try{
                    FileUtils.forceDelete(new File(tmp_file_name));
                }catch (IOException ignored){}
            }
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
            String configurationXML = null;
            try
            {
                configurationXML = this.getXMLFromFile(appName);
            }
            catch(Throwable ignored) {}
            if(configurationXML == null)
            {
                configurationXML = profile.generateContainerConfiguration(serial, mac, appName);
            }
        
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


    private static void LoadFileFromCloud(final String target_filename, final String cloud_object_name)
    throws DemeterException
    {

        final String bucket_name = SystemProperties.getInstance().getStorage().getAwsBucket();

        try {

            MinioClient minioClient = AppManager.getMinioInstance();
            minioClient.downloadObject(
                DownloadObjectArgs.builder()
                .bucket(bucket_name)
                .object(cloud_object_name)
                .filename(target_filename)
                .build());
        
        }
        catch (MinioException e) {
            throw (new DemeterException("Minio exception: " + e + "    HTTP trace: " + e.httpTrace()))
                    .SettHttpStatus(Status.BAD_GATEWAY);
        }
        catch (InvalidKeyException e) {
            throw (new DemeterException("Minio exception: " + e))
                    .SettHttpStatus(Status.SERVICE_UNAVAILABLE);
        }
        catch (IOException e) {
            throw (new DemeterException("Minio exception: " + e))
                    .SettHttpStatus(Status.SERVICE_UNAVAILABLE);
        }
        catch (NoSuchAlgorithmException e){
            throw (new DemeterException("Minio exception: " + e))
                    .SettHttpStatus(Status.SERVICE_UNAVAILABLE);
        }

    }


    public static class FileStreamingOutput implements StreamingOutput 
    {
        private final java.nio.file.Path ipkFilePath;
        private boolean del_file;
        
        public FileStreamingOutput(java.nio.file.Path ipkFilePath, boolean del_after_use)
        {
            this.ipkFilePath = ipkFilePath;
            this.del_file = del_after_use;
        }

        private void delete_file()
        throws IOException
        {
            FileUtils.forceDelete(new File(ipkFilePath.toString()));
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
            finally{
                if(del_file){
                    delete_file();
                }
            }
        }
    }
}
