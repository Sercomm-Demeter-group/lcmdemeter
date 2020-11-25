package com.sercomm.openfire.plugin.data.frontend;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AppCatalog
{
    private String id;
    private String name;
    private Long creationTime;
    
    public AppCatalog()
    {
    }
    
    public static AppCatalog from(ResultSet rs)
    throws SQLException
    {
        AppCatalog object = new AppCatalog();
        
        object.id = Integer.toString(rs.getInt("id"));
        object.name = rs.getString("name");
        object.creationTime = rs.getLong("creationTime");
        
        return object;
    }

    public String getId()
    {
        return this.id;
    }
    
    public void setId(String id)
    {
        this.id = id;
    }
    
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Long getCreationTime()
    {
        return creationTime;
    }

    public void setCreationTime(Long creationTime)
    {
        this.creationTime = creationTime;
    }
}
