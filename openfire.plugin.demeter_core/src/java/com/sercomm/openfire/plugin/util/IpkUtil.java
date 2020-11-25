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
        
        CompressUtil.gzipDecompression(
            tempIPKFilePathString, 
            tempTarFilePathString);
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
            throw new IOException("PACKAGE INFORMATION IS BLANK");
        }
        
        /*
        Package: Plume
        Version: 4
        Filename: Plume_4_arm.ipk
        Section: utils
        Architecture: arm
        Installed-Size: 678650
         */
        Map<String, String> mapper = new HashMap<String, String>();
        String[] lines = controlDataString.split("\n");
        for(String line : lines)
        {
            String[] pair = line.split(":");
            if(pair.length != 2)
            {
                continue;
            }
            
            mapper.put(pair[0].trim().replaceAll("-", XStringUtil.BLANK), pair[1].trim());
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

    /*
    public static com.sercomm.openfire.plugin.model.ipk.Meta validate(Path ipkFilePath)
    throws IOException
    {
        com.sercomm.openfire.plugin.model.ipk.Meta object = null;
        
        if(!Files.exists(ipkFilePath))
        {
            throw new IOException("FILE DOES NOT EXIST: " + ipkFilePath);
        }
        
        // create temporary folder
        Path tempFolder = Files.createTempDirectory(UUID.randomUUID().toString());
        try
        {
            String tempFilename = UUID.randomUUID().toString();
            String tempFolderPath = tempFolder.toAbsolutePath().toString();
            String tempFilePath = tempFolderPath + File.separator + tempFilename;

            // copy from source file
            Files.copy(ipkFilePath, Paths.get(tempFilePath));
            
            final String tempTarFilePath = tempFolderPath + File.separator + tempFilename + ".tar";
            CompressUtil.gzipDecompression(
                tempFilePath, 
                tempTarFilePath);
            
            CompressUtil.tarDecompression(
                tempTarFilePath, 
                tempFolderPath);
            
            // control.tar.gz
            Path controlTarGzFile = Paths.get(tempFolderPath + File.separator + META_FILENAME + ".tar.gz");
            Path controlTarFile = Paths.get(tempFolderPath + File.separator + META_FILENAME + ".tar");
            Path controlFile = Paths.get(tempFolderPath + File.separator + META_FILENAME);
            if(!Files.exists(controlTarGzFile))
            {
                throw new IOException("CONTROL FILE DOES NOT EXIST");
            }
            
            CompressUtil.gzipDecompression(
                controlTarGzFile.toAbsolutePath().toString(), 
                controlTarFile.toAbsolutePath().toString());
            CompressUtil.tarDecompression(
                controlTarFile.toAbsolutePath().toString(), 
                tempFolderPath);
            
            if(!Files.exists(controlFile))
            {
                throw new IOException("CONTROL FILE DOES NOT EXIST");
            }
            
            // read file contents
            byte[] metaData = null;
            File file = controlFile.toAbsolutePath().toFile();
            try(FileInputStream fis = new FileInputStream(file))
            {
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                
                metaData = data;
            }

            String metaDataString = new String(metaData);
            
            Map<String, String> mapper = new HashMap<String, String>();
            String[] lines = metaDataString.split("\n");
            for(String line : lines)
            {
                String[] pair = line.split(":");
                if(pair.length != 2)
                {
                    continue;
                }
                
                mapper.put(pair[0].trim().replaceAll("-", XStringUtil.BLANK), pair[1].trim());
            }
            
            object = new com.sercomm.openfire.plugin.model.ipk.Meta();
            
            object.Package = mapper.get("Package");
            object.Version = mapper.get("Version");
            object.Filename = mapper.get("Filename");
            object.Section = mapper.get("Section");
            object.Architecture = mapper.get("Architecture");
            object.InstalledSize = mapper.get("InstalledSize");
        }
        finally
        {
            FileUtil.forceDeleteDirectory(tempFolder);
        }        
        
        return object;
    }
    */
}
