package com.sercomm.openfire.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.jivesoftware.database.DbConnectionManager;

import com.sercomm.common.util.Algorithm;
import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.data.frontend.AppIcon;
import com.sercomm.openfire.plugin.data.frontend.AppSubscription;
import com.sercomm.openfire.plugin.data.frontend.AppVersion;
import com.sercomm.openfire.plugin.exception.DemeterException;
import com.sercomm.openfire.plugin.util.DbConnectionUtil;
import com.sercomm.openfire.plugin.util.IpkUtil;
import com.sercomm.openfire.plugin.util.StorageUtil;
import com.sercomm.openfire.plugin.define.StorageType;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.Result;
import io.minio.messages.Item;
import okhttp3.HttpUrl;
import io.minio.errors.MinioException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AppManager extends ManagerBase 
{
    //private static final Logger log = LoggerFactory.getLogger(AppManager.class);
    
    private final static String TABLE_S_APP = "sApp";
    private final static String TABLE_S_APP_ICON = "sAppIcon";
    private final static String TABLE_S_APP_VERSION = "sAppVersion";
    private final static String TABLE_S_APP_SUBSCRIPTION = "sAppSubscription";

    private final static String SQL_UPDATE_APP =
            String.format("INSERT INTO `%s`" + 
                "(`id`,`publisher`,`name`,`catalog`,`model`,`price`,`publish`,`description`,`creationTime`) " +
                "VALUES(?,?,?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE `catalog`=?,`price`=?,`publish`=?,`description`=?",
                TABLE_S_APP);
    private final static String SQL_UPDATE_APP_ICON =
            String.format("INSERT INTO `%s`" + 
                "(`appId`,`iconId`,`size`,`updatedTime`,`data`) " +
                "VALUES(?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE `iconId`=?,`size`=?,`updatedTime`=?,`data`=?",
                TABLE_S_APP_ICON);
    private final static String SQL_INSERT_APP_VERSION =
            String.format("INSERT INTO `%s`" + 
                "(`id`,`appId`,`version`,`status`,`filename`,`creationTime`,`ipkFilePath`,`ipkFileSize`,`releaseNote`) " +
                "VALUES(?,?,?,?,?,?,?,?,?)",
                TABLE_S_APP_VERSION);
    private final static String SQL_UPDATE_APP_VERSION =
            String.format("UPDATE `%s` SET " + 
                    "`version`=?,`status`=?,`ipkFileSize`=? " +
                    "WHERE `id`=?",
                    TABLE_S_APP_VERSION);
    private final static String SQL_UPDATE_APP_VERSION_PATH =
            String.format("UPDATE `%s` SET " +
                    "`ipkFilePath`=? " +
                    "WHERE `id`=?",
                    TABLE_S_APP_VERSION);
    private final static String SQL_QUERY_APP =
            String.format("SELECT * FROM `%s` WHERE `id`=?",
                TABLE_S_APP);
    private final static String SQL_QUERY_APPS =
            String.format("SELECT * FROM `%s` ORDER BY `name`",
                TABLE_S_APP);
    private final static String SQL_QUERY_APPS_BY_MODEL =
            String.format("SELECT SQL_CALC_FOUND_ROWS * FROM `%s` WHERE `model`=? ORDER BY ? ? LIMIT ?,?",
                TABLE_S_APP);
    private final static String SQL_DELETE_APP =
            String.format("DELETE FROM `%s` WHERE `id`=?",
                TABLE_S_APP);
    private final static String SQL_QUERY_APP_COUNT_BY_CATALOG =
            String.format("SELECT COUNT(*) AS `count` FROM `%s` WHERE `catalog`=?",
                TABLE_S_APP);
    private final static String SQL_QUERY_APP_VERSION =
            String.format("SELECT * FROM `%s` WHERE `appId`=? AND `version`=?",
                TABLE_S_APP_VERSION);
    private final static String SQL_QUERY_APP_VERSION_BY_ID =
            String.format("SELECT * FROM `%s` WHERE `id`=?",
                TABLE_S_APP_VERSION);
    private final static String SQL_QUERY_APP_VERSIONS =
            String.format("SELECT * FROM `%s` WHERE `appId`=?",
                TABLE_S_APP_VERSION);
    private final static String SQL_QUERY_APP_LATEST_VERSION =
            String.format("SELECT * FROM `%s` WHERE `appId`=? ORDER BY `creationTime` DESC LIMIT 1",
                TABLE_S_APP_VERSION);
    private final static String SQL_DELETE_APP_VERSION =
            String.format("DELETE FROM `%s` WHERE `appId`=? AND `id`=?",
                TABLE_S_APP_VERSION);
    private final static String SQL_QUERY_ALL_APP_VERSIONS =
            String.format("SELECT * FROM `%s`",
                TABLE_S_APP_VERSION);
    private final static String SQL_QUERY_APP_ICON =
            String.format("SELECT * FROM `%s` WHERE `iconId`=?",
                TABLE_S_APP_ICON);
    private final static String SQL_QUERY_APP_ICON_BY_APP_ID =
            String.format("SELECT * FROM `%s` WHERE `appId`=?",
                TABLE_S_APP_ICON);
    private final static String SQL_DELETE_APP_ICON_BY_APP_ID =
            String.format("DELETE FROM `%s` WHERE `appId`=?",
                TABLE_S_APP_ICON);
    private final static String SQL_QUERY_APP_INSTALLED_COUNT =
            String.format("SELECT SUM(`installedCount`) AS `count` FROM `%s` WHERE `appId`=?",
                TABLE_S_APP_VERSION);
    private final static String SQL_QUERY_APP_INSTALLED_COUNT_BY_VERSION =
            String.format("SELECT `installedCount` AS `count` FROM `%s` WHERE `appId`=? AND `version`=?",
                TABLE_S_APP_VERSION);
    private final static String SQL_QUERY_APP_SUBSCRIPTIONS =
            String.format("SELECT * FROM `%s` WHERE `userId`=?",
                TABLE_S_APP_SUBSCRIPTION);
    private final static String SQL_QUERY_APP_SUBSCRIPTION =
            String.format("SELECT * FROM `%s` WHERE `appId`=? AND `userId`=?",
                TABLE_S_APP_SUBSCRIPTION);
    private final static String SQL_INSERT_APP_SUBSCRIPTION =
            String.format("INSERT INTO `%s`(`appId`,`userId`,`creationTime`) VALUES(?,?,?)",
                TABLE_S_APP_SUBSCRIPTION);
    private final static String SQL_DELETE_APP_SUBSCRIPTION =
            String.format("DELETE FROM `%s` WHERE `appId`=? AND `userId`=?",
                TABLE_S_APP_SUBSCRIPTION);
    private final static String SQL_DELETE_APP_SUBSCRIPTIONS =
            String.format("DELETE FROM `%s` WHERE `appId`=?",
                TABLE_S_APP_SUBSCRIPTION);

    private static class AppManagerContainer
    {
        private final static AppManager instance = new AppManager();

        private final static HttpUrl minio_getUrl(){
            HttpUrl url = HttpUrl.parse(SystemProperties.getInstance().getStorage().getAwsUrl());
            return new HttpUrl.Builder()
                .scheme(SystemProperties.getInstance().getStorage().getAwsScheme())
                .host(url.host())
                .port(url.port())
                .build();
        }
        private final static MinioClient minio_instance = MinioClient.builder()
            .endpoint(minio_getUrl())
            .credentials(
                SystemProperties.getInstance().getStorage().getAwsKey(),
                SystemProperties.getInstance().getStorage().getAwsSecret())
            .build();
    }
    
    private AppManager()
    {
    }
    
    public static AppManager getInstance()
    {
        return AppManagerContainer.instance;
    }

    public static MinioClient getMinioInstance()
    {
        return AppManagerContainer.minio_instance;
    }

    @Override
    protected void onInitialize()
    {
    }

    @Override
    protected void onUninitialize()
    {
    }

    
    public static class CloudObj{
        public CloudObj (String fname, String obj_name) {
            filename = fname;
            object_name = obj_name;
        }
        public String filename;
        public String object_name;
    }

    public void SaveFileToCloud(
        String filepath,
        String object_name)
    throws DemeterException
    {
        List<CloudObj> objects = new ArrayList<CloudObj>();
        objects.add(new CloudObj(filepath, object_name));
        SaveFilesToCloud(objects, false);
    }


    public static void SaveFilesToCloud(
        Iterable<CloudObj> objects,
        boolean ignore_errors_flag)
    throws DemeterException
    {

        int saved_num = 0;
        boolean err = true;
        String err_string = "";
        Status http_status = Status.BAD_GATEWAY;


        final String bucket_name = SystemProperties.getInstance().getStorage().getAwsBucket();

        try {
            MinioClient minioClient = getMinioInstance();
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket_name).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket_name).build());
            }

            for(CloudObj obj : objects){
                try{
                    minioClient.uploadObject(
                        UploadObjectArgs.builder()
                        .bucket(bucket_name)
                        .object(obj.object_name)
                        .filename(obj.filename)
                        .build());
                    saved_num++;
                }
                catch(Throwable e){
                    if(ignore_errors_flag == false)
                        throw e;
                }
            }

            err = false;
        }
        catch (MinioException e) {
            err_string = "Minio exception: " + e + "    HTTP trace: " + e.httpTrace();
            http_status = Status.SERVICE_UNAVAILABLE;
        }
        catch (InvalidKeyException e) {
            err_string = "Minio exception: " + e;
        }
        catch (NoSuchAlgorithmException e){
            err_string = "Minio exception: " + e;
        }
        catch(IOException e){
            err_string = "IOException: " + e;
        }

        if(err && !ignore_errors_flag){

            List<String> obj_names = new ArrayList<String>();
            int i = 0;
            for(CloudObj obj : objects){
                if(i == saved_num)
                    break;
                obj_names.add(obj.object_name);
            }
            try{
                RemoveObjectsFromCloud(obj_names);
            }
            catch(DemeterException ignored){
            }

            throw (new DemeterException(err_string)).SettHttpStatus(http_status);
        }

    }


    public static void RemoveObjectFromCloud(
            String object_name)
    throws DemeterException
    {
        List<String> list = new ArrayList<String>();
        list.add(object_name);
        RemoveObjectsFromCloud(list);
    }


    public static void RemoveObjectsFromCloud(
            Iterable<String> objects)
    throws DemeterException
    {

        final String bucket_name = SystemProperties.getInstance().getStorage().getAwsBucket();

         try {

            MinioClient minioClient = getMinioInstance();
            for(String obj : objects){
                minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket_name)
                    .object(obj).build());
            }
            
        }
        catch (MinioException e) {
            throw (new DemeterException("Minio exception: " + e + "    HTTP trace: " + e.httpTrace()))
                    .SettHttpStatus(Status.SERVICE_UNAVAILABLE);
        }
        catch (InvalidKeyException e) {
            throw (new DemeterException("Minio exception: " + e))
                    .SettHttpStatus(Status.BAD_GATEWAY);
        }
        catch (NoSuchAlgorithmException e){
            throw (new DemeterException("Minio exception: " + e))
                    .SettHttpStatus(Status.BAD_GATEWAY);
        }
        catch(IOException e){
            throw (new DemeterException("IOException: " + e))
                    .SettHttpStatus(Status.BAD_GATEWAY);
        }

    }


    public static void RemoveFolderFromCloud(
            String folder_name)
    throws DemeterException
    {
        
        if(folder_name.length() == 0){
            return;
        }

        if(false == folder_name.endsWith(File.separator)){
            folder_name = folder_name.concat(File.separator);
        }

         try {

            final String bucket_name = SystemProperties.getInstance().getStorage().getAwsBucket();

            MinioClient minioClient = getMinioInstance();

            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                .bucket(bucket_name)
                .prefix(folder_name)
                .build());
        
            for (Result<Item> result : results) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket_name)
                    .object(result.get().objectName())
                    .build());
            }
        
        }
        catch (MinioException e) {
            throw (new DemeterException("Minio exception: " + e + "    HTTP trace: " + e.httpTrace()))
                    .SettHttpStatus(Status.SERVICE_UNAVAILABLE);
        }
        catch (InvalidKeyException e) {
            throw (new DemeterException("Minio exception: " + e))
                    .SettHttpStatus(Status.BAD_GATEWAY);
        }
        catch (NoSuchAlgorithmException e){
            throw (new DemeterException("Minio exception: " + e))
                    .SettHttpStatus(Status.BAD_GATEWAY);
        }
        catch(IOException e){
            throw (new DemeterException("IOException: " + e))
                    .SettHttpStatus(Status.BAD_GATEWAY);
        }

    }
    
 
    public void addApp(
            String publisher,
            String name,
            String catalog,
            String modelName,
            String price,
            Integer publish,
            String description,
            byte[] iconData)
    throws DemeterException, Throwable
    {
        App application = null;
        try
        {
            application = this.getApp(publisher, name, modelName);
        }
        catch(DemeterException ignored) {}

        if(null != application)
        {
            throw new DemeterException("APPLICATION ALREADY EXISTS");
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        boolean abort = false;
        try
        {
            final String id = Algorithm.md5(publisher + name + modelName);
            final long creationTime = System.currentTimeMillis();
            
            conn = DbConnectionManager.getConnection();
            conn = DbConnectionUtil.openTransaction(conn);
            do
            {
                int idx;

                // update first table
                stmt = conn.prepareStatement(SQL_UPDATE_APP);
                                
                idx = 0;
                // `id`,`publisher`,`name`,`catalog`,`modelName`,`price`,`publish`,`description`,`creationTime`
                stmt.setString(++idx, id);
                stmt.setString(++idx, publisher);
                stmt.setString(++idx, name);
                stmt.setString(++idx, catalog);
                stmt.setString(++idx, modelName);
                stmt.setString(++idx, price);
                stmt.setInt(++idx, publish);
                stmt.setString(++idx, description);
                stmt.setLong(++idx, creationTime);
                
                // `catalog`,`price`,`publish`,`description`
                stmt.setString(++idx, catalog);
                stmt.setString(++idx, price);
                stmt.setInt(++idx, publish);
                stmt.setString(++idx, description);
                
                stmt.executeUpdate();
                DbConnectionManager.fastcloseStmt(stmt);
                
                if(null == iconData)
                {
                    break;
                }
                
                // update 2nd table
                stmt = conn.prepareStatement(SQL_UPDATE_APP_ICON);

                final String appId = id;
                final String iconId = Algorithm.md5(iconData);
                
                idx = 0;
                // `appId`,`iconId`,`size`,`updatedTime`,`data`
                stmt.setString(++idx, appId);
                stmt.setString(++idx, iconId);
                stmt.setLong(++idx, iconData.length);
                stmt.setLong(++idx, creationTime);
                stmt.setBytes(++idx, iconData);
                
                // `iconId`,`size`,`updatedTime`,`data`
                stmt.setString(++idx, iconId);
                stmt.setLong(++idx, iconData.length);
                stmt.setLong(++idx, creationTime);
                stmt.setBytes(++idx, iconData);
                
                stmt.executeUpdate();
            }
            while(false);
        }
        catch(Throwable t)
        {
            abort = true;
            throw t;
        }
        finally
        {
            DbConnectionManager.closeStatement(stmt);
            DbConnectionUtil.closeTransaction(conn, abort);
            DbConnectionManager.closeConnection(conn);
        }
    }

    public void setApp(
            App object, 
            byte[] iconData)
    throws DemeterException, Throwable
    {
        final String id = object.getId();
        if(null == this.getApp(id))
        {
            throw new DemeterException("APP DOES NOT EXIST");
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        boolean abort = false;
        try
        {
            conn = DbConnectionManager.getConnection();
            conn = DbConnectionUtil.openTransaction(conn);
            do
            {
                int idx;
                
                // update 1st table
                idx = 0;
                stmt = conn.prepareStatement(SQL_UPDATE_APP);                    
                // `id`,`publisher`,`name`,`catalog`,`model`,`price`,`publish`,`description`,`creationTime`
                stmt.setString(++idx, id);
                stmt.setString(++idx, object.getPublisher());
                stmt.setString(++idx, object.getName());
                stmt.setString(++idx, object.getCatalog());
                stmt.setString(++idx, object.getModelName());
                stmt.setString(++idx, object.getPrice());
                stmt.setInt(++idx, object.getPublish());
                stmt.setString(++idx, object.getDescription());
                stmt.setLong(++idx, object.getCreationTime());                
                // `catalog`,`price`,`publish`,`description`
                stmt.setString(++idx, object.getCatalog());
                stmt.setString(++idx, object.getPrice());
                stmt.setInt(++idx, object.getPublish());
                stmt.setString(++idx, object.getDescription());
                
                stmt.executeUpdate();
                DbConnectionManager.fastcloseStmt(stmt);
                
                if(null == iconData)
                {
                    break;
                }
                                
                // update 2nd table
                final long updatedTime = System.currentTimeMillis();
                final String appId = id;
                
                // generate icon ID
                int length = iconData.length + Long.BYTES;
                ByteBuffer buffer = ByteBuffer.allocate(length);
                buffer.put(iconData);
                buffer.putLong(updatedTime);
                final String iconId = Algorithm.md5(buffer.array());

                idx = 0;
                stmt = conn.prepareStatement(SQL_UPDATE_APP_ICON);
                // `appId`,`iconId`,`size`,`updatedTime`,`data`
                stmt.setString(++idx, appId);
                stmt.setString(++idx, iconId);
                stmt.setLong(++idx, iconData.length);
                stmt.setLong(++idx, updatedTime);
                stmt.setBytes(++idx, iconData);                
                // `iconId`,`size`,`updatedTime`,`data`
                stmt.setString(++idx, iconId);
                stmt.setLong(++idx, iconData.length);
                stmt.setLong(++idx, updatedTime);
                stmt.setBytes(++idx, iconData);                
                stmt.executeUpdate();
            }
            while(false);
        }
        catch(Throwable t)
        {
            abort = true;
            throw t;
        }
        finally
        {
            DbConnectionManager.closeStatement(stmt);
            DbConnectionUtil.closeTransaction(conn, abort);
            DbConnectionManager.closeConnection(conn);
        }
    }

    public List<App> getApps()
    throws DemeterException, Throwable
    {
        List<App> apps = new ArrayList<App>();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APPS);
            
            rs = stmt.executeQuery();
            while(rs.next())
            {
                App object = App.from(rs);
                apps.add(object);
            }
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }
        
        return apps;
    }
    
    public List<App> getAppsByModel(String modelName)
    throws DemeterException, Throwable
    {
        List<App> apps = new ArrayList<App>();
        
        this.getAppsByModel(modelName, 0, 9999, "name", "asc", apps);
        
        return apps;
    }
    
    public int getAppsByModel(
            String modelName, 
            Integer from, 
            Integer size, 
            String sort, 
            String order,
            List<App> apps)
    throws DemeterException, Throwable
    {
        int totalCount = 0;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APPS_BY_MODEL);
            
            int idx = 0;
            stmt.setString(++idx, modelName);
            stmt.setString(++idx, sort);
            stmt.setString(++idx, order);
            stmt.setInt(++idx, from);
            stmt.setInt(++idx, size);
            
            rs = stmt.executeQuery();
            while(rs.next())
            {
                App object = App.from(rs);
                apps.add(object);
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
        
        return totalCount;
    }
    
    public App getApp(
            String publisher,
            String name,
            String modelName)
    throws DemeterException, Throwable
    {
        if(XStringUtil.isBlank(publisher) || 
           XStringUtil.isBlank(name))
        {
            throw new DemeterException("ARGUMENT(S) CANNOT BE BLANK");
        }
        final String id = Algorithm.md5(publisher + name + modelName);

        return this.getApp(id);
    }
    
    public App getApp(String appId)
    throws DemeterException, Throwable
    {
        App object = null;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP);
            
            int idx = 0;
            stmt.setString(++idx, appId);
            
            rs = stmt.executeQuery();
            do
            {
                if(!rs.next())
                {
                    throw new DemeterException("APP CANNOT BE FOUND");
                }
                
                object = App.from(rs);
            }
            while(false);
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }
        
        return object;
    }
    
    public void deleteApp(
            String appId)
    throws DemeterException, Throwable
    {
        // obtain the App object and check if the App ID is valid
        App app = this.getApp(appId);
                
        // backup the App's versions information at first
        List<AppVersion> appVersions = this.getAppVersions(app.getId());

        boolean abort = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            conn = DbConnectionUtil.openTransaction(conn);

            // delete versions of the application
            for(AppVersion appVersion: appVersions)
            {
                this.deleteAppVersion(app.getId(), appVersion.getId());
            }

            // delete App subscriptions
            this.deleteAppSubscriptions(conn, app.getId());
            
            // delete App icon
            this.deleteAppIcon(conn, app.getId());
            
            // delete the App
            int idx;
            idx = 0;
            stmt = conn.prepareStatement(SQL_DELETE_APP);                        
            stmt.setString(++idx, app.getId());            
            stmt.executeUpdate();
        }
        catch(Throwable t)
        {
            abort = true;
            throw t;
        }
        finally
        {
            DbConnectionManager.closeStatement(stmt);
            DbConnectionUtil.closeTransaction(conn, abort);
            DbConnectionManager.closeConnection(conn);
        }
        
        // delete its physical files
        if(false == abort)
        {

            final StorageType storage_type = SystemProperties.getInstance().getStorage().getStorageType();

            if(storage_type == StorageType.LOCAL_FS){

                if(false == appVersions.isEmpty())
                {
                    StringBuilder sb = new StringBuilder(SystemProperties.getInstance().getStorage().getRootPath());
                    if(sb.toString().endsWith(File.separator) == false){
                        sb.append(File.separator);
                    }
                    sb.append(appId);

                    try
                    {
                        FileUtils.forceDelete(new File(sb.toString()));
                    }
                    catch(Throwable ignored) {}

                }

            }
        }
    }

    public void deleteAppVersion(
            String appId,
            String versionId)
    throws DemeterException, Throwable
    {
        AppVersion appVersion = this.getAppVersion(versionId);
        
        boolean abortTransaction = true;
        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            // delete the row from database
            conn = DbConnectionManager.getConnection();
            DbConnectionUtil.openTransaction(conn);
            stmt = conn.prepareStatement(SQL_DELETE_APP_VERSION);

            int idx = 0;
            stmt.setString(++idx, appId);
            stmt.setString(++idx, versionId);
            stmt.executeUpdate();
            
            final StorageType storage_type = SystemProperties.getInstance().getStorage().getStorageType();

            if(storage_type == StorageType.LOCAL_FS){

                // delete the package's folder from volume
                File ipkFile = new File(
                     SystemProperties.getInstance().getStorage().getRootPath() +
                     File.separator +
                     appVersion.getIPKFilePath());
                
                FileUtils.forceDelete(ipkFile.getParentFile());

            } else if(storage_type == StorageType.AWS_S3){

                File ipkFile = new File(appVersion.getIPKFilePath());
                RemoveFolderFromCloud(ipkFile.getParentFile().toString());

            } else {

                throw new DemeterException("STORAGE TYPE IS NOT SUPPORTED");

            }

            abortTransaction = false;
        }
        catch(Throwable t)
        {
            throw t;
        }
        finally
        {
            DbConnectionManager.closeStatement(stmt);
            DbConnectionUtil.closeTransaction(conn, abortTransaction);
            DbConnectionManager.closeConnection(conn);
        }
    }

    private void deleteAppIcon(
            Connection conn,
            String appId)
    throws DemeterException, Throwable
    {
        PreparedStatement stmt = null;
        try
        {
            stmt = conn.prepareStatement(SQL_DELETE_APP_ICON_BY_APP_ID);
            
            int idx = 0;
            stmt.setString(++idx, appId);
            
            stmt.executeUpdate();
        }
        finally
        {
            DbConnectionManager.closeStatement(stmt);
        }
    }

    private void deleteAppSubscriptions(
            Connection conn,
            String appId)
    throws DemeterException, Throwable
    {
        PreparedStatement stmt = null;
        try
        {
            stmt = conn.prepareStatement(SQL_DELETE_APP_SUBSCRIPTIONS);
            
            int idx = 0;
            stmt.setString(++idx, appId);
            
            stmt.executeUpdate();
        }
        finally
        {
            DbConnectionManager.closeStatement(stmt);
        }
    }

    public Long getAppCount(
            String catalogName)
    throws DemeterException, Throwable
    {
        Long count = 0L;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_COUNT_BY_CATALOG);
            
            int idx = 0;
            stmt.setString(++idx, catalogName);
            
            rs = stmt.executeQuery();
            if(rs.next())
            {
                count = rs.getLong("count");
            }
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }
        
        return count;
    }

    public void addAppVersion(
            String applicationId,
            String versionName,
            Integer status, // 1: enable, 0: disable
            String ipkFileName,
            byte[] ipkFileData)
    throws DemeterException, Throwable
    {
        App app = this.getApp(applicationId);
        if(null == app)
        {
            throw new DemeterException("APPLICATION DOES NOT EXIST");
        }

        if(XStringUtil.isBlank(versionName))
        {
            //throw new DemeterException("VERSION NAME CANNOT BE BLANK");
            // !!! added temporary version name as there is no version here (due to a bug)
            versionName = "version_" + String.valueOf(System.currentTimeMillis());
        }
        
        // create temporary folder
        Path tempFolder = Files.createTempDirectory(UUID.randomUUID().toString());
        try
        {
            Path tempIPKFilePath = Paths.get(
                tempFolder.toAbsolutePath().toString() + File.separator + IpkUtil.PACKAGE_IPK_FILENAME);

            // save bytes array as "data.ipk" file to temporary folder
            try(FileOutputStream fos = new FileOutputStream(tempIPKFilePath.toFile()))
            {
                fos.write(ipkFileData);
            }

            // extract the IPK, and verify its package information
            // finally, a "Packages.gz" file will be left in the temporary folder
            com.sercomm.openfire.plugin.data.ipk.Meta meta = IpkUtil.validate(tempIPKFilePath);
            if(XStringUtil.isBlank(meta.Version))
            {
                throw new DemeterException("BAD PACKAGE INFORMATION. NO REQUIRED FIELD WITHIN 'control' FILE OF THE PACKAGE: 'Version'");
            }

            // a "Packages.gz" file will be generated after IpkUtil.validate() method being executed
            final Path tempPackageInfoFilePath =
                    Paths.get(tempFolder.toAbsolutePath().toString() + File.separator + IpkUtil.PACKAGE_GZ_FILENAME);
            
            final long creationTime = System.currentTimeMillis();

            // generate version ID
            final String versionId = generateVersionId(
                app.getId(),
                app.getModelName(),
                creationTime);

            // --> begin filename investigation
            // 1. if there is no specific IPK filename, use the filename within
            //    package information by default 
            if(XStringUtil.isBlank(ipkFileName))
            {
                ipkFileName = meta.Filename;
            }
            
            // 2. if IPK filename is still blank, set the filename same as application's name
            if(XStringUtil.isBlank(ipkFileName))
            {
                ipkFileName = app.getName();
            }            
            // --> end filename investigation
            
            final String description = XStringUtil.isBlank(meta.Description) ? XStringUtil.BLANK : meta.Description;

            AppVersion appVersion = null;
            // check if duplicate version name exists
            try
            {
                appVersion = this.getAppVersion(applicationId, versionName);
            }
            catch(DemeterException ignored) {}
            finally
            {
                if(null != appVersion)
                {
                    throw new DemeterException("APP VERSION ALREADY EXISTS");
                }
            }

            // check if duplicate version ID exists
            try
            {
                appVersion = this.getAppVersion(versionId);
            }
            catch(DemeterException ignored) {}
            finally
            {
                if(null != appVersion)
                {
                    throw new DemeterException("APP VERSION ALREADY EXISTS");
                }
            }

            final StorageType storage_type = SystemProperties.getInstance().getStorage().getStorageType();
            
            final String VersionPathString = app.getId() + File.separator + versionId;
            String FSRootPathString = SystemProperties.getInstance().getStorage().getRootPath();
            if(FSRootPathString.endsWith(File.separator) == false){
                FSRootPathString = FSRootPathString.concat(File.separator);
            }

            final String ipkFilePathString = VersionPathString + File.separator + IpkUtil.PACKAGE_IPK_FILENAME;
            final String packageInfoFilePathString = VersionPathString + File.separator + IpkUtil.PACKAGE_GZ_FILENAME;

           
            // 1. establish database records 
            // 2. move files
            boolean abortTransaction = true;
            Connection conn = null;
            PreparedStatement stmt = null;
            try
            {
                conn = DbConnectionManager.getConnection();
                DbConnectionUtil.openTransaction(conn);
                stmt = conn.prepareStatement(SQL_INSERT_APP_VERSION);
                
                int idx = 0;
                // `id`,`appId`,`version`,`status`,`creationTime`,`ipkFilePath`,`ipkFileSize`,`releaseNote`
                stmt.setString(++idx, versionId);
                stmt.setString(++idx, applicationId);
                stmt.setString(++idx, versionName);
                stmt.setInt(++idx, status);
                stmt.setString(++idx, ipkFileName);
                stmt.setLong(++idx, creationTime);
                stmt.setString(++idx, ipkFilePathString);
                stmt.setLong(++idx, ipkFileData.length);
                stmt.setString(++idx, description);
                stmt.executeUpdate();
                
                // database record being inserted successfully
                // then moving the files
                
                boolean moved = false;
                try
                {
                    if(storage_type == StorageType.LOCAL_FS){

                        final Path packageFolderPath = Paths.get(FSRootPathString + VersionPathString);
                        final Path ipkFilePath = Paths.get(FSRootPathString + ipkFilePathString);
                        final Path packageInfoFilePath = Paths.get(FSRootPathString + packageInfoFilePathString);
                    
                        try
                        {
                            // check if the package folder exists
                            if(false == Files.exists(packageFolderPath))
                            {
                                Files.createDirectories(packageFolderPath.toAbsolutePath());
                            }
                        }
                        catch(IOException e)
                        {
                            throw new IOException("FAILED TO CREATE DIRECTORY: " + e.getMessage());
                        }

                        try
                        {
                            // move IPK file
                            Files.move(tempIPKFilePath, ipkFilePath);
                            // move package info. file
                            Files.move(tempPackageInfoFilePath, packageInfoFilePath);

                        }
                        catch(IOException e)
                        {
                            throw new IOException("FAILED TO MOVE IPK FILE TO VOLUME: " + e.getMessage());
                        }
                    
                    } else if(storage_type == StorageType.AWS_S3){

                        List<CloudObj> objects = new ArrayList<CloudObj>();
                        objects.add(new CloudObj(tempIPKFilePath.toString(), ipkFilePathString));
                        objects.add(new CloudObj(tempPackageInfoFilePath.toString(), packageInfoFilePathString));
                        SaveFilesToCloud(objects, false);

                    } else {

                        throw new DemeterException("STORAGE TYPE IS NOT SUPPORTED");

                    }

                    moved = true;                 
                    
                }
                finally
                {
                    
                    // fallback
                    if(false == moved)
                    {
                        if(storage_type == StorageType.LOCAL_FS){

                            // catch surrounded since the folder path might not be created yet
                            try
                            {
                                final Path VPath = Paths.get(FSRootPathString + VersionPathString);
                                FileUtils.forceDelete(VPath.toFile());
                            }
                            catch(Throwable ignored) {}
                            }
                    }
                    
                }

                abortTransaction = false;
            }
            finally
            {
                DbConnectionManager.closeStatement(stmt);
                DbConnectionUtil.closeTransaction(conn, abortTransaction);
                DbConnectionManager.closeConnection(conn);
            }            
        }
        finally
        {
            // clean temporary files
            FileUtils.forceDelete(tempFolder.toFile());
        }
    }

    public void updateAppVersion(
            String applicationId,
            String versionId,
            String versionName,
            Integer status, // 1: enable, 0: disable
            String ipkFileName,
            byte[] ipkFileData)
    throws DemeterException, Throwable
    {
        App app = this.getApp(applicationId);
        if(null == app)
        {
            throw new DemeterException("APPLICATION DOES NOT EXIST");
        }

        if(XStringUtil.isBlank(versionName))
        {
            throw new DemeterException("VERSION NAME CANNOT BE BLANK");
        }

        if(0 != status && 1 != status)
        {
            throw new DemeterException("INVALID 'status' VALUE");
        }

        AppVersion appVersion = this.getAppVersion(versionId);

        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_UPDATE_APP_VERSION);
            
            // `version`=?,`status`=?,`ipkFileSize`=? 
            int idx = 0;
            stmt.setString(++idx, versionName);
            stmt.setInt(++idx, status);
            stmt.setLong(++idx, null == ipkFileData ? appVersion.getIPKFileSize() : ipkFileData.length);
            stmt.setString(++idx, appVersion.getId());

            stmt.executeUpdate();
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }

        // update the IPK file if necessary
        if(null != ipkFileData)
        {
            // create temporary folder
            Path tempFolder = Files.createTempDirectory(
                UUID.randomUUID().toString());
            try
            {
                Path tempIPKFilePath = Paths.get(
                    tempFolder.toAbsolutePath().toString() + File.separator + IpkUtil.PACKAGE_IPK_FILENAME);

                // save bytes array as "data.ipk" file to temporary folder
                try(FileOutputStream fos = new FileOutputStream(tempIPKFilePath.toFile()))
                {
                    fos.write(ipkFileData);
                }

                // extract the IPK, and verify its package information
                // finally, a "Packages.gz" file will be left in the temporary folder
                com.sercomm.openfire.plugin.data.ipk.Meta meta = IpkUtil.validate(tempIPKFilePath);

                // --> begin filename investigation
                // 1. if there is no specific IPK filename, use the filename within
                //    package information by default 
                if(XStringUtil.isBlank(ipkFileName))
                {
                    ipkFileName = meta.Filename;
                }
                
                // 2. if IPK filename is still blank, set the filename same as application's name
                if(XStringUtil.isBlank(ipkFileName))
                {
                    ipkFileName = app.getName();
                }            
                // --> end filename investigation

                // a "Packages.gz" file will be generated after IpkUtil.validate() method being executed
                final Path tempPackageInfoFilePath =
                        Paths.get(tempFolder.toAbsolutePath().toString() + File.separator + IpkUtil.PACKAGE_GZ_FILENAME);
                
                String FSRootPathString = SystemProperties.getInstance().getStorage().getRootPath();
                if(FSRootPathString.endsWith(File.separator) == false){
                    FSRootPathString = FSRootPathString.concat(File.separator);
                }

                final String VersionPathString = app.getId() + File.separator + versionId;

                final String ipkFilePathString = VersionPathString + File.separator + IpkUtil.PACKAGE_IPK_FILENAME;
                final String packageInfoFilePathString = VersionPathString + File.separator + IpkUtil.PACKAGE_GZ_FILENAME;

                final StorageType storage_type = SystemProperties.getInstance().getStorage().getStorageType();
            
                if(storage_type == StorageType.LOCAL_FS){

                    final Path packageFolderPath = Paths.get(FSRootPathString + VersionPathString);
                    final Path ipkFilePath = Paths.get(FSRootPathString + ipkFilePathString);
                    final Path packageInfoFilePath = Paths.get(FSRootPathString + packageInfoFilePathString);
                                    
                    try
                    {
                        // check if the package folder exists
                        if(Files.exists(packageFolderPath))
                        {
                            // force delete the original folder
                            FileUtils.forceDelete(packageFolderPath.toFile());
                        }

                        // re-create it
                        Files.createDirectories(packageFolderPath);
                    }
                    catch(IOException e)
                    {
                        throw new IOException("FAILED TO CREATE DIRECTORY: " + e.getMessage());
                    }

                    try
                    {
                        // move IPK file
                        Files.move(tempIPKFilePath, ipkFilePath);
                        // move package info. file
                        Files.move(tempPackageInfoFilePath, packageInfoFilePath);
                    }
                    catch(IOException e)
                    {
                        throw new IOException("FAILED TO MOVE IPK FILE TO VOLUME: " + e.getMessage());
                    }
    
                } else if(storage_type == StorageType.AWS_S3){

                    RemoveFolderFromCloud(VersionPathString);
                    List<CloudObj> objects = new ArrayList<CloudObj>();
                    objects.add(new CloudObj(tempIPKFilePath.toString(), ipkFilePathString));
                    objects.add(new CloudObj(tempPackageInfoFilePath.toString(), packageInfoFilePathString));
                    SaveFilesToCloud(objects, false);

                 } else {

                    throw new DemeterException("STORAGE TYPE IS NOT SUPPORTED");

                }            
                
            }
            finally
            {
                // clean temporary files
                FileUtils.forceDelete(tempFolder.toFile());
            }
        }
    }

    public AppVersion getAppVersion(
            String appId,
            String version)
    throws DemeterException, Throwable
    {
        AppVersion object = null;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_VERSION);
            
            int idx = 0;
            stmt.setString(++idx, appId);
            stmt.setString(++idx, version);
            
            rs = stmt.executeQuery();
            if(!rs.next())
            {
                throw new DemeterException("APP VERSION WAS NOT FOUND");
            }
            object = AppVersion.from(rs);
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }
        
        return object;
    }
    
    public AppVersion getAppVersion(
            String versionId)
    throws DemeterException, Throwable
    {
        AppVersion object = null;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_VERSION_BY_ID);
            
            int idx = 0;
            stmt.setString(++idx, versionId);
            
            rs = stmt.executeQuery();
            if(!rs.next())
            {
                throw new DemeterException("APP VERSION WAS NOT FOUND");
            }
            object = AppVersion.from(rs);
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }
        
        return object;
    }
    
    public AppVersion getAppLatestVersion(
            String appId)
    throws DemeterException, Throwable
    {
        AppVersion object = null;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_LATEST_VERSION);
            
            int idx = 0;
            stmt.setString(++idx, appId);
            
            rs = stmt.executeQuery();
            if(!rs.next())
            {
                throw new DemeterException("NO APP VERSION AVAILABLE");
            }
            object = AppVersion.from(rs);
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }
        
        return object;
    }
    
    public List<AppVersion> getAppVersions(String appId)
    throws DemeterException, Throwable
    {
        List<AppVersion> appVersions = null;
        
        Connection conn = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            
            appVersions = this.getAppVersions(conn, appId);
        }
        finally
        {
            DbConnectionManager.closeConnection(conn);
        }

        return appVersions;
    }
    
    private List<AppVersion> getAppVersions(Connection conn, String appId)
    throws DemeterException, Throwable
    {
        List<AppVersion> appVersions = new ArrayList<AppVersion>();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            stmt = conn.prepareStatement(SQL_QUERY_APP_VERSIONS);
            
            int idx = 0;
            stmt.setString(++idx, appId);
            
            rs = stmt.executeQuery();
            while(rs.next())
            {
                AppVersion object = AppVersion.from(rs);
                appVersions.add(object);
            }
        }
        finally
        {
            DbConnectionManager.closeStatement(rs, stmt);
        }
        
        return appVersions;
    }
    
    public AppIcon getAppIcon(
            String iconId)
    throws DemeterException, Throwable
    {
        AppIcon object = null;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_ICON);
            
            int idx = 0;
            stmt.setString(++idx, iconId);
            
            rs = stmt.executeQuery();
            /*
            if(!rs.next())
            {
                throw new DemeterException("APP ICON WAS NOT FOUND");
            }
            object = AppIcon.from(rs);
            */
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }
        
        return object;
    }
    
    public AppIcon getAppIconByAppId(
            String appId)
    throws DemeterException, Throwable
    {
        AppIcon object = null;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_ICON_BY_APP_ID);
            
            int idx = 0;
            stmt.setString(++idx, appId);
            
            rs = stmt.executeQuery();
            /*
            if(!rs.next())
            {
                throw new DemeterException("APP ICON WAS NOT FOUND");
            }
            object = AppIcon.from(rs);
            */
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }
        
        return object;
    }
    
    public long getAppInstalledCount(
            String appId)
    throws DemeterException, Throwable
    {
        long count = 0;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_INSTALLED_COUNT);
            
            int idx = 0;
            stmt.setString(++idx, appId);
            
            rs = stmt.executeQuery();
            
            if(rs.next())
            {
                count = rs.getLong("count");
            }
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }

        return count;
    }
    
    public long getAppInstalledCount(
            String appId,
            String version)
    throws DemeterException, Throwable
    {
        long count = 0;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_INSTALLED_COUNT_BY_VERSION);
            
            int idx = 0;
            stmt.setString(++idx, appId);
            stmt.setString(++idx, version);
            
            rs = stmt.executeQuery();
            if(rs.next())
            {
                count = rs.getLong("count");
            }
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }

        return count;
    }
    
    public List<AppSubscription> getSubscribedApps(
            String userId)
    throws DemeterException, Throwable
    {
        List<AppSubscription> collection = new ArrayList<AppSubscription>();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_SUBSCRIPTIONS);
            
            int idx = 0;
            stmt.setString(++idx, userId);
            
            rs = stmt.executeQuery();
            while(rs.next())
            {
                AppSubscription object = AppSubscription.from(rs);
                collection.add(object);
            }
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }

        return collection;
    }

    public AppSubscription getSubscribedApp(
            String appId,
            String userId)
    throws DemeterException, Throwable
    {
        AppSubscription object = null;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_SUBSCRIPTION);
            
            int idx = 0;
            stmt.setString(++idx, appId);
            stmt.setString(++idx, userId);
            
            rs = stmt.executeQuery();
            do
            {
                if(!rs.next())
                {
                    break;
                }
                
                object = AppSubscription.from(rs);
            }
            while(false);
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }

        return object;
    }
    
    public void subscribeApp(String appId, String userId)
    throws DemeterException, Throwable
    {
        AppSubscription object = this.getSubscribedApp(appId, userId);
        if(null != object)
        {
            throw new DemeterException("APP HAS ALREADY BEEN SUBSCRIBED");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_INSERT_APP_SUBSCRIPTION);
            
            int idx = 0;
            stmt.setString(++idx, appId);
            stmt.setString(++idx, userId);
            stmt.setLong(++idx, System.currentTimeMillis());
            
            stmt.executeUpdate();
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }        
    }
    
    public void unsubscribeApp(String appId, String userId)
    throws DemeterException, Throwable
    {
        AppSubscription object = this.getSubscribedApp(appId, userId);
        if(null == object)
        {
            throw new DemeterException("APP HAS NOT BEEN SUBSCRIBED");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_DELETE_APP_SUBSCRIPTION);

            int idx = 0;
            stmt.setString(++idx, appId);
            stmt.setString(++idx, userId);

            stmt.executeUpdate();
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }                
    }
    
    private String generateVersionId(
            String applicationId,
            String modelName,
            Long creationTime)
    {
        byte[] applicationIdData = applicationId.getBytes(StandardCharsets.UTF_8);
        byte[] modelNameData = modelName.getBytes(StandardCharsets.UTF_8);

        int length = applicationIdData.length + modelNameData.length + Long.BYTES;

        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.put(applicationIdData);
        buffer.put(modelNameData);
        buffer.putLong(creationTime);

        return  Algorithm.md5(buffer.array());        
    }


    public static void exportAppsToCloud()
    throws DemeterException, Throwable
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        List<CloudObj> file_objs = new ArrayList<CloudObj>();

        class update_item{
            String id;
            String path;
            update_item(String _id, String _path){
                id = _id;
                path = _path;
            }
        };

        List<update_item> update_app_versions = new ArrayList<update_item>();

        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_ALL_APP_VERSIONS);
            rs = stmt.executeQuery();
            while(rs.next()){
                AppVersion object = AppVersion.from(rs);
                try{
                    appendFileObjs(object.getIPKFilePath(), file_objs);
                }
                catch(Throwable ignored){}

                String new_path = getRelativePath(object);
                if(new_path.equals(object.getIPKFilePath()) == false ){
                    update_app_versions.add(new update_item(object.getId(), new_path));
                }
            }

            for(update_item version : update_app_versions){
                stmt = conn.prepareStatement(SQL_UPDATE_APP_VERSION_PATH);
                stmt.setString(1, version.path);
                stmt.setString(2, version.id);
                stmt.executeUpdate();
            }

        }
        catch(SQLException e){
            throw (new DemeterException("SQL exception: " + e.getMessage())).SettHttpStatus(Status.INTERNAL_SERVER_ERROR);
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }

        if(file_objs.size() > 0){
            SaveFilesToCloud(file_objs, true);
        }

    }


    private static void appendFileObjs(
        String ipk_file_path,
        List<CloudObj> objs)
    throws Throwable
    {
        String root_path = "";
        String file_path = "";

        root_path = SystemProperties.getInstance().getStorage().getRootPath();
        if(root_path.endsWith(File.separator) == false){
            root_path = root_path.concat(File.separator);
        }

        if(ipk_file_path.startsWith(File.separator) == false){
            file_path = root_path + ipk_file_path;
        } else {
            file_path = ipk_file_path;
            if(file_path.startsWith(root_path) == false){
                // system setting for root path do not match root path in DB entry
                File path = new File(file_path);
                root_path = path.getParentFile().getParentFile().getParentFile().toString();
                if(root_path.endsWith(File.separator) == false){
                    root_path = root_path.concat(File.separator);
                }
            }
        }

        //find parent (app version) folder
        File ipkFile = new File(file_path);

        //collect all file names in folder
        List<String> all_files = null;

        try (Stream<Path> walk = Files.walk(ipkFile.getParentFile().toPath())) {

            all_files = walk.filter(Files::isRegularFile)
                .filter(file -> !Files.isDirectory(file))
                .map(x -> x.toString())
                .collect(Collectors.toList());

        } catch (IOException ignored) {}

        String version_path = ipkFile.getParentFile().toString();
        if(version_path.endsWith(File.separator) == false){
            version_path = version_path.concat(File.separator);
        }

        // add object for each found file
        if(all_files != null){

            for(String file : all_files){
                objs.add(new CloudObj(file, file.substring(root_path.length())));
            }

        }

    }


    private static String getRelativePath(
        AppVersion version){

        String app_str  = version.getAppId();
        String cur_path = version.getIPKFilePath();

        if(cur_path.startsWith(File.separator) == false
            && cur_path.startsWith(app_str) == true){
            return cur_path;
        }

        return cur_path.substring(cur_path.indexOf(app_str));
    }


}
