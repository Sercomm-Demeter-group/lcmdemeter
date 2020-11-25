package com.sercomm.openfire.plugin.data.frontend;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EndUser
{
    private String id;
    private String role;
    private String storedKey;
    private String encryptedPassword;
    private Integer valid;
    private Long creationTime;
    
    public static EndUser from(ResultSet rs)
    throws SQLException
    {
        EndUser object = new EndUser();
        
        object.id = rs.getString("id");
        object.setRole(
            rs.getString("role"));
        object.setStoredKey(
            rs.getString("storedKey"));
        object.setEncryptedPassword(
            rs.getString("encryptedPassword"));
        object.setValid(
            rs.getInt("valid"));
        object.setCreationTime(
            rs.getLong("creationTime"));
        
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

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getStoredKey()
    {
        return storedKey;
    }

    public void setStoredKey(String storedKey)
    {
        this.storedKey = storedKey;
    }

    public String getEncryptedPassword()
    {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword)
    {
        this.encryptedPassword = encryptedPassword;
    }

    public Integer getValid()
    {
        return valid;
    }

    public void setValid(Integer valid)
    {
        this.valid = valid;
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
