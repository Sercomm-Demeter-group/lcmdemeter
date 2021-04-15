package com.sercomm.openfire.plugin.util;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbConnectionUtil
{
    private static final Logger log = LoggerFactory.getLogger(DbConnectionUtil.class);

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
            log.error(t.getMessage(), t);
        }
    }
}
