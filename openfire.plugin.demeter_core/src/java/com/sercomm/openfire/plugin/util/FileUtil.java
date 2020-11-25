package com.sercomm.openfire.plugin.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtil
{
    public static void forceDeleteDirectory(File directory) 
    throws IOException 
    {
        if(!directory.isDirectory()) 
        {
            throw new IOException("NOT DIRECTORY " + directory);
        }

        File[] entries = directory.listFiles();
        if(entries != null) 
        {
            for(File entry : entries) 
            {
                forceDeleteDirectory(entry);
            }
        }
        
        if(!directory.delete()) 
        {
            throw new IOException("FAILED TO DELETE " + directory);
        }
    }
    
    public static void forceDeleteDirectory(
            Path directory) 
    throws IOException 
    {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() 
        {
            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attrs) 
            throws IOException 
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
 
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException 
            {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
