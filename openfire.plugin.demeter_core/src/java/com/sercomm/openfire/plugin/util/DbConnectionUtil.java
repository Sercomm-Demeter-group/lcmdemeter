package com.sercomm.openfire.plugin.util;

import java.sql.Connection;
import java.sql.SQLException;

import com.sercomm.commons.util.Log;

public class DbConnectionUtil
{
    public static Connection openTransaction(Connection connection)
    throws SQLException
    {
        connection.setAutoCommit(false);
        return connection;
    }
    
    public static void closeTransaction(Connection connection, boolean abort)
    {
        try
        {
            if(null == connection)
            {
                return;
            }

            try
            {
                if(abort) 
                {
                    connection.rollback();
                }
                else 
                {
                    connection.commit();
                }
            }
            finally
            {
                connection.setAutoCommit(true);
            }
        }
        catch(Throwable t)
        {
            Log.write().error(t.getMessage(), t);
        }
    }
}
