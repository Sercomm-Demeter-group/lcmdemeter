package com.sercomm.openfire.plugin.data.frontend;

import java.sql.ResultSet;
import java.sql.SQLException;

public class App
{
    private String id;
    private String publisher;
    private String name;
    private String catalog;
    private String modelName;
    private String price;
    private Long creationTime;
    private Integer publish;
    private String description;
    
    public App()
    {
    }
    
    public static App from(ResultSet rs)
    throws SQLException
    {
        App object = new App();
        
        object.id = rs.getString("id");
        object.publisher = rs.getString("publisher");
        object.name = rs.getString("name");
        object.catalog = rs.getString("catalog");
        object.setModelName(rs.getString("model"));
        object.price = rs.getString("price");
        object.publish = rs.getInt("publish");
        object.description = rs.getString("description");
        object.creationTime = rs.getLong("creationTime");
        
        return object;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getPublisher()
    {
        return publisher;
    }

    public void setPublisher(String publisher)
    {
        this.publisher = publisher;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCatalog()
    {
        return catalog;
    }

    public void setCatalog(String catalog)
    {
        this.catalog = catalog;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getPrice()
    {
        return price;
    }

    public void setPrice(String price)
    {
        this.price = price;
    }

    public Long getCreationTime()
    {
        return creationTime;
    }

    public void setCreationTime(Long creationTime)
    {
        this.creationTime = creationTime;
    }

    public Integer getPublish()
    {
        return publish;
    }

    public void setPublish(Integer publish)
    {
        this.publish = publish;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
