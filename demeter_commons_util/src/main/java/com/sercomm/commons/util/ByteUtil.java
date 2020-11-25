package com.sercomm.commons.util;

import java.util.Arrays;

public class ByteUtil
{
    private final static char[] hexArray = "0123456789abcdef".toCharArray();
    
    public static String bytesToHexString(byte[] bytes) 
    {
        char[] hexChars = new char[bytes.length * 2];
        for(int j = 0; j < bytes.length; j++ ) 
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        
        return new String(hexChars);
    }
    
    public static String bytesToHexString(byte[] bytes, int position, int length)
    {
        byte[] byteArray = new byte[length];
        System.arraycopy(bytes, position, byteArray, 0, length);
        
        return bytesToHexString(byteArray);
    }
    
    public static byte[] trimByteArray(byte[] bytes)
    {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }
    
    public static byte[] hexStringToByteArray(String dataString) 
    {
        int length = dataString.length();
        byte[] data = new byte[length / 2];
        for (int idx = 0; idx < length; idx += 2) 
        {
            data[idx / 2] = (byte) ((Character.digit(dataString.charAt(idx), 16) << 4)
                                 + Character.digit(dataString.charAt(idx + 1), 16));
        }
        
        return data;
    }
}
