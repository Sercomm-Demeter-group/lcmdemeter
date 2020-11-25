package com.sercomm.openfire.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.database.DbConnectionManager;

import com.sercomm.common.util.ManagerBase;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.data.frontend.AppCatalog;
import com.sercomm.openfire.plugin.exception.DemeterException;

public class AppCatalogManager extends ManagerBase 
{
    private final static String TABLE_S_APP_CATALOG = "sAppCatalog";

    private final static String SQL_INSERT_APP_CATALOG =
            String.format("INSERT INTO `%s`(`name`,`creationTime`) VALUES(?,?)",
                TABLE_S_APP_CATALOG);
    private final static String SQL_UPDATE_APP_CATALOG =
            String.format("UPDATE `%s` SET `name`=? WHERE `id`=?",
                TABLE_S_APP_CATALOG);
    private final static String SQL_QUERY_APP_CATALOGS =
            String.format("SELECT `id`,`name`,`creationTime` FROM `%s` ORDER BY `name`",
                TABLE_S_APP_CATALOG);
    private final static String SQL_QUERY_APP_CATALOG =
            String.format("SELECT `id`,`name`,`creationTime` FROM `%s` WHERE `id`=?",
                TABLE_S_APP_CATALOG);
    private final static String SQL_QUERY_APP_CATALOG_BY_NAME =
            String.format("SELECT `id`,`name`,`creationTime` FROM `%s` WHERE `name`=?",
                TABLE_S_APP_CATALOG);
    private final static String SQL_DELETE_APP_CATALOG =
            String.format("DELETE FROM `%s` WHERE `id`=?",
                TABLE_S_APP_CATALOG);

    private final static class CategoryManagerContainer
    {
        private final static AppCatalogManager instance = new AppCatalogManager();
    }
    
    private AppCatalogManager()
    {
    }
    
    public static AppCatalogManager getInstance()
    {
        return CategoryManagerContainer.instance;
    }

    @Override
    protected void onInitialize()
    {
    }

    @Override
    protected void onUninitialize()
    {
    }
    
    public void addCatalog(String name)
    throws DemeterException, Throwable
    {
        if(XStringUtil.isBlank(name))
        {
            throw new DemeterException("ARGUMENT(S) CANNOT BE BLANK");
        }
        
        if(null != this.getCatalog(name))
        {
            throw new DemeterException("CATALOG ALREADY EXISTS");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_INSERT_APP_CATALOG);
            
            int idx = 0;
            stmt.setString(++idx, name);
            stmt.setLong(++idx, System.currentTimeMillis());
            
            stmt.executeUpdate();
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }
    }
    
    public AppCatalog getCatalog(Integer id)
    throws DemeterException, Throwable
    {
        AppCatalog object = null;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_CATALOG);
            
            int idx = 0;
            stmt.setInt(++idx, id);
            
            rs = stmt.executeQuery();
            do
            {
                if(false == rs.first())
                {
                    break;
                }
                
                object = AppCatalog.from(rs);
            }
            while(false);
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }
        
        return object;
    }
    
    public AppCatalog getCatalog(String name)
    throws DemeterException, Throwable
    {
        AppCatalog object = null;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_CATALOG_BY_NAME);
            
            int idx = 0;
            stmt.setString(++idx, name);
            
            rs = stmt.executeQuery();
            do
            {
                if(false == rs.first())
                {
                    break;
                }
                
                object = AppCatalog.from(rs);
            }
            while(false);
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }
        
        return object;
    }
    
    public List<AppCatalog> getCatalogs()
    throws DemeterException, Throwable
    {
        List<AppCatalog> catalogs = new ArrayList<AppCatalog>();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_QUERY_APP_CATALOGS);
            
            rs = stmt.executeQuery();
            do
            {
                if(false == rs.first())
                {
                    break;
                }
                
                do
                {
                    AppCatalog object = AppCatalog.from(rs);
                    catalogs.add(object);
                }
                while(rs.next());
            }
            while(false);
        }
        finally
        {
            DbConnectionManager.closeConnection(rs, stmt, conn);
        }
        
        return catalogs;
    }
    
    public void updateCatalog(AppCatalog object)
    throws DemeterException, Throwable
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_UPDATE_APP_CATALOG);
            
            int idx = 0;
            stmt.setString(++idx, object.getName());
            stmt.setInt(++idx, Integer.parseInt(object.getId()));

            stmt.executeUpdate();
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }
    }
    
    public void deleteCatalog(AppCatalog object)
    throws DemeterException, Throwable
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        try
        {
            conn = DbConnectionManager.getConnection();
            stmt = conn.prepareStatement(SQL_DELETE_APP_CATALOG);
            
            int idx = 0;
            stmt.setInt(++idx, Integer.parseInt(object.getId()));

            stmt.executeUpdate();
        }
        finally
        {
            DbConnectionManager.closeConnection(stmt, conn);
        }
    }
}
