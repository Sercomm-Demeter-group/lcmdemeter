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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

public class AppManager extends ManagerBase 
{
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
    }
    
    private AppManager()
    {
    }
    
    public static AppManager getInstance()
    {
        return AppManagerContainer.instance;
    }

    @Override
    protected void onInitialize()
    {
    }

    @Override
    protected void onUninitialize()
    {
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
        if(null != this.getApp(publisher, name, modelName))
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
            if(false == appVersions.isEmpty())
            {
                for(AppVersion appVersion : appVersions)
                {
                    String versionId = appVersion.getVersion();
                    
                    String folderPath = StorageUtil.Path.makePackageFolderPath(
                        SystemProperties.getInstance().getStorage().getRootPath(), appId, versionId);
                
                    try
                    {
                        FileUtils.forceDelete(new File(folderPath));
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
            
            // delete the package's folder from volume
            File ipkFile = new File(appVersion.getIPKFilePath());
            FileUtils.forceDelete(ipkFile.getParentFile());

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
            throw new DemeterException("VERSION NAME CANNOT BE BLANK");
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
            
            final String packageFolderPathString = StorageUtil.Path.makePackageFolderPath(
                SystemProperties.getInstance().getStorage().getRootPath(), 
                app.getId(), 
                versionId);
            
            final Path packageFolderPath =
                    Paths.get(packageFolderPathString);
            final Path ipkFilePath = 
                    Paths.get(packageFolderPathString + File.separator + IpkUtil.PACKAGE_IPK_FILENAME);
            final Path packageInfoFilePath = 
                    Paths.get(packageFolderPathString + File.separator + IpkUtil.PACKAGE_GZ_FILENAME);
            
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
                stmt.setString(++idx, ipkFilePath.toAbsolutePath().toString());
                stmt.setLong(++idx, ipkFileData.length);
                stmt.setString(++idx, description);
                stmt.executeUpdate();
                
                // database record being inserted successfully
                // then moving the files
                
                boolean moved = false;
                try
                {
                    try
                    {
                        // check if the package folder exists
                        if(false == Files.exists(packageFolderPath))
                        {
                            Files.createDirectories(packageFolderPath);
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
                    
                    moved = true;
                }
                finally
                {
                    // fallback
                    if(false == moved)
                    {
                        // catch surrounded since the folder path might not be created yet
                        try
                        {
                            FileUtils.forceDelete(packageFolderPath.toFile());
                        }
                        catch(Throwable ignored) {}
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
                
                final String packageFolderPathString = StorageUtil.Path.makePackageFolderPath(
                    SystemProperties.getInstance().getStorage().getRootPath(), 
                    appVersion.getAppId(), 
                    versionId);
                
                final Path packageFolderPath =
                        Paths.get(packageFolderPathString);
                final Path ipkFilePath = 
                        Paths.get(packageFolderPathString + File.separator + IpkUtil.PACKAGE_IPK_FILENAME);
                final Path packageInfoFilePath = 
                        Paths.get(packageFolderPathString + File.separator + IpkUtil.PACKAGE_GZ_FILENAME);
                
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
            if(!rs.next())
            {
                throw new DemeterException("APP ICON WAS NOT FOUND");
            }
            object = AppIcon.from(rs);
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
            if(!rs.next())
            {
                throw new DemeterException("APP ICON WAS NOT FOUND");
            }
            object = AppIcon.from(rs);
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
}
