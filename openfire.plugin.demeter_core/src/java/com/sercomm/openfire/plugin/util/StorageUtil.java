package com.sercomm.openfire.plugin.util;

import java.io.File;

public class StorageUtil
{
    public final static class Path
    {
        public static String makePackageFolderPath(
                String rootPath,
                String appId,
                String versionId)
        {
            StringBuilder builder = new StringBuilder();
            
            builder.append(rootPath);
            if(false == rootPath.endsWith(File.separator))
            {
                builder.append(File.separator);
            }
            
            builder.append(appId)
                   .append(File.separator)
                   .append(versionId);
            
            return builder.toString();
        }
    }
}
