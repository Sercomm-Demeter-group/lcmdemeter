package com.sercomm.openfire.plugin.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

public class CompressUtil
{
    public static boolean tarCompression(
            String[] filesPathArray, 
            String resultFilePath) 
    throws IOException 
    {
        FileOutputStream fos = null;
        TarArchiveOutputStream taos = null;
        try {
            fos = new FileOutputStream(new File(resultFilePath));
            taos = new TarArchiveOutputStream(fos);
            for (String filePath : filesPathArray) {
                BufferedInputStream bis = null;
                FileInputStream fis = null;
                try {
                    File file = new File(filePath);
                    TarArchiveEntry tae = new TarArchiveEntry(file);
                    tae.setName(new String(file.getName().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
                    taos.putArchiveEntry(tae);
                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    int count;
                    byte data[] = new byte[1024];
                    while ((count = bis.read(data, 0, 1024)) != -1) {
                        taos.write(data, 0, count);
                    }
                } finally {
                    taos.closeArchiveEntry();
                    if (bis != null) 
                        bis.close();
                    if (fis != null) 
                        fis.close();
                }
            } 
        } finally {
            if (taos != null) 
                taos.close();
            if (fos != null) 
                fos.close();
            
        }
        return true;
    }
 
    public static boolean tarDecompression(
            String decompressFilePath, 
            String resultDirPath) 
    throws IOException 
    {
        TarArchiveInputStream tais = null;
        FileInputStream fis = null;
        try 
        {
            File file = new File(decompressFilePath);
            fis = new FileInputStream(file);
            tais = new TarArchiveInputStream(fis);
            TarArchiveEntry tae = null;
            while ((tae = tais.getNextTarEntry()) != null) 
            {
                BufferedOutputStream bos = null;
                FileOutputStream fos = null;
                try 
                {
                    if(new File(tae.getName()).isDirectory())
                    {
                        continue;
                    }
                    
                    String dir = resultDirPath + File.separator + tae.getName();// tar档中文件
                    File dirFile = new File(dir);
                    fos = new FileOutputStream(dirFile);
                    bos = new BufferedOutputStream(fos);
                    int count;
                    byte[] data = new byte[1024];
                    while ((count = tais.read(data, 0, 1024)) != -1) 
                    {
                        bos.write(data, 0, count);
                    }
                } 
                finally 
                {
                    if (bos != null) 
                        bos.close();
                    if (fos != null) 
                        fos.close();
                }
            } 
        } 
        finally 
        {
            if(tais != null)
                tais.close();
            if(fis != null) 
                fis.close();
        }
        return true;
    }
 
    public static boolean gzipCompression(
            String filePath, 
            String resultFilePath) 
    throws IOException 
    {
        InputStream fin = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos= null;
        GzipCompressorOutputStream gcos = null;
        try {
            fin = Files.newInputStream(Paths.get(filePath));
            bis = new BufferedInputStream(fin);
            fos = new FileOutputStream(resultFilePath);
            bos = new BufferedOutputStream(fos);
            gcos = new GzipCompressorOutputStream(bos);
            byte[] buffer = new byte[1024];
            int read = -1;
            while ((read = bis.read(buffer)) != -1) {
                gcos.write(buffer, 0, read);
            }
        } finally {
            if(gcos != null)
                gcos.close();
            if(bos != null)
                bos.close();
            if(fos != null)
                fos.close();
            if(bis != null)
                bis.close();
            if(fin != null)
                fin.close();
        }
        return true;
    }
 
    public static boolean gzipDecompression(
            String compressedFilePath, 
            String resultDirPath) 
    throws IOException 
    {
        InputStream fin = null;
        BufferedInputStream in = null;
        OutputStream out = null;
        GzipCompressorInputStream gcis = null;
        try {
            out = Files.newOutputStream(Paths.get(resultDirPath));
            fin = Files.newInputStream(Paths.get(compressedFilePath));
            in = new BufferedInputStream(fin);
            gcis = new GzipCompressorInputStream(in);
            final byte[] buffer = new byte[1024];
            int n = 0;
            while (-1 != (n = gcis.read(buffer))) {
                out.write(buffer, 0, n);
            } 
        } finally {
            if(gcis != null)
                gcis.close();
            if(in != null)
                in.close();
            if(fin != null)
                fin.close();
            if(out != null)
                out.close();
        }
        return true;
    }
    
    public static boolean zipCompression(
            String[] filesPathArray, 
            String resultFilePath) 
    throws IOException 
    {
        ZipArchiveOutputStream zaos = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(resultFilePath));
            zaos = new ZipArchiveOutputStream(fos);
            for (String filePath : filesPathArray) {
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                try {
                    File file = new File(filePath);
                    ZipArchiveEntry zae = new ZipArchiveEntry(file, file.getName());
                    zaos.putArchiveEntry(zae);
                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    int count;
                    byte data[] = new byte[1024];
                    while ((count = bis.read(data, 0, 1024)) != -1) {
                        zaos.write(data, 0, count);
                    }
                } finally {
                    zaos.closeArchiveEntry();
                    if (bis != null)
                        bis.close();
                    if (fis != null)
                        fis.close();
                }
 
            } 
        } finally {
            if (zaos != null)
                zaos.close();
            if (fos != null)
                fos.close();
        }
        return true;
    }
 
    public static boolean zipDecompression(
            String decompressFilePath, 
            String resultDirPath) 
    throws IOException 
    {
        ZipArchiveInputStream zais = null;
        FileInputStream fis = null;
        try {
            File file = new File(decompressFilePath);
            fis = new FileInputStream(file);
            zais = new ZipArchiveInputStream(fis);
            ZipArchiveEntry zae = null;
            while ((zae = zais.getNextZipEntry()) != null) {
                FileOutputStream fos = null;
                BufferedOutputStream bos = null;
                try {
                    String dir = resultDirPath + File.separator + zae.getName();// tar档中文件
                    File dirFile = new File(dir);
                    fos = new FileOutputStream(dirFile);
                    bos = new BufferedOutputStream(fos);
                    int count;
                    byte data[] = new byte[1024];
                    while ((count = zais.read(data, 0, 1024)) != -1) {
                        bos.write(data, 0, count);
                    }
                } finally {
                    if (bos != null) 
                        bos.close();
                    if (fos != null)
                        fos.close();
                }
            } 
        } finally {
            if (zais != null)
                zais.close();
            if (fis != null)
                fis.close();
        }
        return true;
    }
}
