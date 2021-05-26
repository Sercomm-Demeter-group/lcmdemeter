package com.sercomm.openfire.plugin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.sercomm.commons.util.XStringUtil;

public class IpkUtil
{
    public final static String CONTROL_FILENAME = "control";
    public final static String PACKAGE_GZ_FILENAME = "Packages.gz";
    public final static String PACKAGE_IPK_FILENAME = "package.ipk";
    
    public static com.sercomm.openfire.plugin.data.ipk.Meta validate(Path tempIPKFilePath)
    throws IOException
    {
        com.sercomm.openfire.plugin.data.ipk.Meta object = null;
        
        if(!Files.exists(tempIPKFilePath))
        {
            throw new IOException("TEMP IPK FILE DOES NOT EXIST: " + tempIPKFilePath);
        }
        
        String tempIPKFilePathString = tempIPKFilePath.toAbsolutePath().toString();
        Path tempFolderPath = tempIPKFilePath.getParent();
        String tempFolderPathString = tempFolderPath.toAbsolutePath().toString();
        final String tempTarFilePathString = tempFolderPath + File.separator + tempIPKFilePath.getFileName() + ".tar";
        
        // check if the IPK file was packed by ZIP or GZIP
        // if both ZIP and GZIP cannot extract the IPK file
        // then throwing an error
        try
        {
            // try ZIP format at first
            CompressUtil.zipDecompression(
                tempIPKFilePathString, 
                tempTarFilePathString);
        }
        catch(IOException ignored)
        {
            // try GZIP format instead
            try
            {
                CompressUtil.gzipDecompression(
                    tempIPKFilePathString, 
                    tempTarFilePathString);                
            }
            catch(IOException e)
            {
                // interrupt and throw the exception
                throw e;
            }
        }

        CompressUtil.tarDecompression(
            tempTarFilePathString, 
            tempFolderPathString);
        
        // control.tar.gz
        Path controlTarGzFile = Paths.get(tempFolderPathString + File.separator + CONTROL_FILENAME + ".tar.gz");
        // control.tar
        Path controlTarFile = Paths.get(tempFolderPathString + File.separator + CONTROL_FILENAME + ".tar");
        // control
        Path controlFile = Paths.get(tempFolderPathString + File.separator + CONTROL_FILENAME);
        
        if(!Files.exists(controlTarGzFile))
        {
            throw new IOException("'control.tar.gz' FILE DOES NOT EXIST");
        }
        
        // to control.tar
        CompressUtil.gzipDecompression(
            controlTarGzFile.toAbsolutePath().toString(), 
            controlTarFile.toAbsolutePath().toString());
        // to control
        CompressUtil.tarDecompression(
            controlTarFile.toAbsolutePath().toString(), 
            tempFolderPathString);
        
        if(!Files.exists(controlFile))
        {
            throw new IOException("'control' FILE DOES NOT EXIST");
        }

        // read contents of control file
        String controlDataString = XStringUtil.BLANK;
        try(FileInputStream fis = new FileInputStream(
                controlFile.toAbsolutePath().toFile()))
        {
            byte[] data = new byte[(int) Files.size(controlFile)];
            fis.read(data);
            
            controlDataString = new String(data);
        }

        if(XStringUtil.isBlank(controlDataString))
        {
            throw new IOException("PACKAGE CONTROL FILE IS BLANK");
        }

        // format of control file
        /*
         *   Package: Plume\n
         *   Version: 4\n
         *   Filename: Plume_4_arm.ipk\n
         *   Section: utils\n
         *   Architecture: arm\n
         *   Installed-Size: 678650\n
         *   Description: ...\n
         *   \n
         */
        // or
        /*
         *   Package: web_hello\n
         *   Version: 1\n
         *   Filename: \n
         *   Section: utils\n
         *   Architecture: mips\n
         *   Installed-Size: 16818\n
         *    web test\n
         *   \n
         */
        Map<String, String> mapper = new HashMap<String, String>();
        String[] lines = controlDataString.split("\n");
        for(String line : lines)
        {
            String[] tokens = line.split(":");
            if(tokens.length == 2)
            {
                mapper.put(tokens[0].trim().replaceAll("-", XStringUtil.BLANK), tokens[1].trim());
            }
            else
            {
                mapper.put("Description", tokens[0].trim());
            }
        }
        
        object = new com.sercomm.openfire.plugin.data.ipk.Meta();
        
        object.Package = mapper.get("Package");
        object.Version = mapper.get("Version");
        object.Filename = mapper.get("Filename");
        object.Section = mapper.get("Section");
        object.Architecture = mapper.get("Architecture");
        object.InstalledSize = mapper.get("InstalledSize");
        object.Description = mapper.get("Description");
        
        // gzip the control file as "Packages.gz"
        Path packageGzPath = Paths.get(tempFolderPathString + File.separator + PACKAGE_GZ_FILENAME);
        CompressUtil.gzipCompression(
            controlFile.toAbsolutePath().toString(), 
            packageGzPath.toAbsolutePath().toString());
        
        return object;
    }
}
