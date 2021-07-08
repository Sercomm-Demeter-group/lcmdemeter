package com.sercomm.openfire.plugin.service.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.AppManager;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.OwnershipManager;
import com.sercomm.openfire.plugin.cache.DeviceCache;
import com.sercomm.openfire.plugin.cache.EndUserCache;
import com.sercomm.openfire.plugin.cache.OwnershipCache;
import com.sercomm.openfire.plugin.data.frontend.App;
import com.sercomm.openfire.plugin.data.frontend.AppCatalog;
import com.sercomm.openfire.plugin.data.frontend.AppEventRecord;
import com.sercomm.openfire.plugin.data.frontend.AppIcon;
import com.sercomm.openfire.plugin.data.frontend.AppInstallation;
import com.sercomm.openfire.plugin.data.frontend.AppVersion;
import com.sercomm.openfire.plugin.define.AppEventType;
import com.sercomm.openfire.plugin.define.AppState;
import com.sercomm.openfire.plugin.define.DeviceState;
import com.sercomm.openfire.plugin.define.EndUserRole;
import com.sercomm.openfire.plugin.exception.DemeterException;

public class ServiceAPIUtil
{
    public final static class Admin
    {
        public static com.sercomm.openfire.plugin.service.dto.admin.App convert(
                App app)
        throws DemeterException, Throwable
        {
            final Boolean enable = app.getPublish() == 0 ? false : true;
            
            com.sercomm.openfire.plugin.service.dto.admin.App object = 
                    new com.sercomm.openfire.plugin.service.dto.admin.App();
            object.id = app.getId();
            object.name = app.getName();
            object.enable = enable;
            object.created_at = app.getCreationTime().toString();
            object.catalog = app.getCatalog();
            object.device_model = app.getModelName();
            object.download = AppManager.getInstance().getAppInstalledCount(app.getId());
            object.company = app.getPublisher();
            object.price = app.getPrice();
            object.desc = app.getDescription();
            
            List<AppVersion> versions = AppManager.getInstance().getAppVersions(app.getId());
            for(AppVersion version : versions)
            {
                final String versionVal = version.getVersion();
    
                /*
                public String id = XStringUtil.EMPTY;
                public String redirect_url = XStringUtil.EMPTY;
        
                public Boolean enable;
                public String created_at;
                public String app_id;
                public String updated_at;
                public String version;
                public Long download;
                public String release_note;
                 */
                                            
                com.sercomm.openfire.plugin.service.dto.admin.Version versionModel = 
                        new com.sercomm.openfire.plugin.service.dto.admin.Version();
                versionModel.enable = enable;
                versionModel.created_at = version.getCreationTime().toString();
                versionModel.app_id = version.getAppId();
                versionModel.updated_at = version.getCreationTime().toString();
                versionModel.version = versionVal;
                versionModel.download = AppManager.getInstance().getAppInstalledCount(app.getId(), versionVal);
                versionModel.release_note = String.format("%s (ID: %s)", version.getReleaseNote(), version.getId());
                
                object.versions.add(versionModel);
                /*
                public String id;
                public String app_id;
                public String checksum;
                public String created_at;
                public String updated_at;
                public String file_size;
                public String location;
                 */
                com.sercomm.openfire.plugin.service.dto.admin.File fileModel = 
                        new com.sercomm.openfire.plugin.service.dto.admin.File();
                fileModel.id = version.getId();
                fileModel.app_id = version.getAppId();
                fileModel.checksum = version.getId();
                fileModel.created_at = version.getCreationTime().toString();
                fileModel.updated_at = version.getCreationTime().toString();
                fileModel.file_size = version.getIPKFileSize().toString();
                fileModel.location = version.getIPKFilePath();
                
                object.files.add(fileModel);
            }
            
            AppIcon appIcon = null;
            try
            {
                appIcon = AppManager.getInstance().getAppIconByAppId(app.getId());
            }
            // it will be fine if no icon presents
            catch(Throwable ignored) {}

            if(null != appIcon)
            {
                com.sercomm.openfire.plugin.service.dto.admin.File fileModel = 
                        new com.sercomm.openfire.plugin.service.dto.admin.File();
                fileModel.res_type = "app";
                fileModel.file_type = "icon";
                fileModel.minetype = "image/jpeg";
                
                fileModel.id = appIcon.getIconId();
                fileModel.app_id = app.getId();
                fileModel.checksum = appIcon.getIconId();
                fileModel.created_at = appIcon.getUpdatedTime().toString();
                fileModel.updated_at = appIcon.getUpdatedTime().toString();
                fileModel.file_size = appIcon.getSize().toString();
                
                object.files.add(fileModel);
            }
            
            return object;
        }
    
        public static com.sercomm.openfire.plugin.service.dto.admin.Catalog convert(
                AppCatalog appCatalog)
        throws DemeterException, Throwable
        {
            com.sercomm.openfire.plugin.service.dto.admin.Catalog object = 
                    new com.sercomm.openfire.plugin.service.dto.admin.Catalog();
            
            object.id = appCatalog.getId();               
            object.name = appCatalog.getName();
            object.created_at = appCatalog.getCreationTime().toString();
            object.count = AppManager.getInstance().getAppCount(appCatalog.getName()).toString();
            
            return object;
        }
    
        public static com.sercomm.openfire.plugin.service.dto.admin.Device convert(
                DeviceCache deviceCache)
        throws DemeterException, Throwable
        {
            com.sercomm.openfire.plugin.service.dto.admin.Device object = 
                    new com.sercomm.openfire.plugin.service.dto.admin.Device();
            
            object.id = deviceCache.getUID();
            object.serial = deviceCache.getSerial();
            object.mac = NameRule.formatMac(deviceCache.getMac());
            object.model = deviceCache.getModelName();
            object.name = deviceCache.getCustomName();
            object.created_at = Long.toString(deviceCache.getCreationTime());
            object.updated_at = Long.toString(deviceCache.getLastOnlineTime());
            object.fw_ver = deviceCache.getFirmwareVersion();
            object.link = (0 == DeviceState.ONLINE.compareTo(deviceCache.getDeviceState())) ? true : false; 
            object.enable = deviceCache.getEnable();
            
            return object;
        }
        
        public static com.sercomm.openfire.plugin.service.dto.admin.Customer convert(
                EndUserCache endUser)
        throws DemeterException, Throwable
        {
            com.sercomm.openfire.plugin.service.dto.admin.Customer object =
                    new com.sercomm.openfire.plugin.service.dto.admin.Customer();
            
            object.id = endUser.getId();
            object.serial = endUser.getId();
            object.name = endUser.getId();
            object.admin = (0 == EndUserRole.ADMIN.compareTo(endUser.getEndUserRole())) ? true : false;
            object.enable = (1 == endUser.getValid()) ? true : false;
            object.created_at = endUser.getCreationTime().toString();
            
            Map<String, OwnershipCache> collection = 
                    OwnershipManager.getInstance().getOwnerships(endUser.getId());
            Iterator<String> iterator = collection.keySet().iterator();
            while(iterator.hasNext())
            {
                final String nodeName = iterator.next();
                final String serial = NameRule.toDeviceSerial(nodeName);
                final String mac = NameRule.toDeviceMac(nodeName);
                
                DeviceCache device = DeviceManager.getInstance().getDeviceCache(serial, mac);
                object.devices.add(convert(device));
            }
            
            return object;
        }
    }
    
    public final static class V1
    {
        public static com.sercomm.openfire.plugin.service.dto.v1.App convert(
                App app,
                String userId,
                String serial,
                String mac)
        throws DemeterException, Throwable
        {
            com.sercomm.openfire.plugin.service.dto.v1.App object = 
                    new com.sercomm.openfire.plugin.service.dto.v1.App();

            String appId = app.getId();
            Long download = AppManager.getInstance().getAppInstalledCount(appId);
            AppIcon icon = AppManager.getInstance().getAppIconByAppId(appId);
            AppVersion version = AppManager.getInstance().getAppLatestVersion(appId);
            
            AppInstallation appInstallation = null;
            try
            {
                appInstallation = DeviceManager.getInstance().getInstalledApp(serial, mac, app.getName());
            }
            catch(Throwable ignored) {}
            
            object.id = appId;
            object.uuid = XStringUtil.BLANK;
            object.company = app.getPublisher();
            object.name = app.getName();
            object.catalog = app.getCatalog();
            object.modelName = app.getModelName();
            object.created_at = app.getCreationTime().toString();            
            object.desc = app.getDescription();
            //object.icon_url = null != icon ? String.format("/api/v1/files/icon/%s", icon.getIconId()) : XStringUtil.EMPTY;       
            // TODO: fix path issue on portal page (portal page inject '/icon' into the above URL
            object.icon_url = null != icon ? String.format("/api/v1/files/%s", icon.getIconId()) : XStringUtil.BLANK;       
            object.version = null != version ? version.getVersion() : XStringUtil.BLANK;
            object.price = app.getPrice();

            // TODO
            object.cpu = "0.0%";
            object.memory = "0 KB";
            object.storage = "0.0 MB";
            
            object.installed = null == appInstallation ? false : true;
            object.purchased = null == AppManager.getInstance().getSubscribedApp(appId, userId) ? false : true;
            // TODO
            object.upgrade = false; 

            if(true == object.installed)
            {
                object.status = (1 == appInstallation.getExecuted()) ? AppState.RUNNING.toString() : AppState.INSTALLED.toString();
            }
            else
            {
                object.status = XStringUtil.BLANK;
            }

            object.download = download;

            return object;
        }
        
        public static com.sercomm.openfire.plugin.service.dto.v1.Customer convert(
                EndUserCache endUserCache)
        throws DemeterException, Throwable
        {
            com.sercomm.openfire.plugin.service.dto.v1.Customer object = 
                    new com.sercomm.openfire.plugin.service.dto.v1.Customer();

            object.name = endUserCache.getId();
            object.serial = endUserCache.getId();
            
            return object;
        }

        public static com.sercomm.openfire.plugin.service.dto.v1.Device convert(
                DeviceCache deviceCache)
        throws DemeterException, Throwable
        {
            com.sercomm.openfire.plugin.service.dto.v1.Device object = 
                    new com.sercomm.openfire.plugin.service.dto.v1.Device();

            object.serial = deviceCache.getSerial();
            object.mac = NameRule.formatMac(deviceCache.getMac());
            object.company = deviceCache.getCompany();
            object.model = deviceCache.getModelName();
            object.name = deviceCache.getSerial();
            object.link = (0 == DeviceState.ONLINE.compareTo(deviceCache.getDeviceState())) ? true : false;
            
            return object;
        }

        public static com.sercomm.openfire.plugin.service.dto.v1.Identity convert(
                EndUserCache endUserCache,
                DeviceCache deviceCache)
        throws DemeterException, Throwable
        {
            com.sercomm.openfire.plugin.service.dto.v1.Identity object = 
                    new com.sercomm.openfire.plugin.service.dto.v1.Identity();

            object.customer = convert(endUserCache);
            object.device = null == deviceCache ? null : convert(deviceCache);

            return object;
        }
        
        public static com.sercomm.openfire.plugin.service.dto.v1.Catalog convert(
                AppCatalog appCatalog)
        throws DemeterException, Throwable
        {
            com.sercomm.openfire.plugin.service.dto.v1.Catalog object = 
                    new com.sercomm.openfire.plugin.service.dto.v1.Catalog();
            
            object.id = appCatalog.getId();               
            object.name = appCatalog.getName();
            object.created_at = appCatalog.getCreationTime().toString();
            object.count = AppManager.getInstance().getAppCount(appCatalog.getName()).toString();
            
            return object;
        }
        
        public static com.sercomm.openfire.plugin.service.dto.v1.Event convert(
                AppEventRecord appEventRecord)
        throws DemeterException, Throwable
        {
            com.sercomm.openfire.plugin.service.dto.v1.Event object =
                    new com.sercomm.openfire.plugin.service.dto.v1.Event();
            
            AppEventType appEventType = AppEventType.fromString(appEventRecord.getEventType());
            
            object.id = appEventRecord.getId();
            object.app_id = appEventRecord.getAppId();
            object.app_name = appEventRecord.getAppName();
            object.dev_id = NameRule.formatDeviceName(appEventRecord.getSerial(), appEventRecord.getMac());
            object.customer_id = appEventRecord.getUserId();
            object.action = appEventType.toString();
            object.msg = appEventType.toMessage();
            object.created_at = appEventRecord.getUnixTime().toString();
            
            return object;
        }
    }
}
