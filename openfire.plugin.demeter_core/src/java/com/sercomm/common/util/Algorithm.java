package com.sercomm.common.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class Algorithm
{
    private final static String MD5_PATTERN = "00000000000000000000000000000000";
    public static String md5(String input)
    {
        return md5(input.getBytes(StandardCharsets.US_ASCII));
    }

    public static String md5(byte[] data)
    {
        String md5String = null;
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);

            md5String = new BigInteger(1, md.digest()).toString(16);
            md5String = MD5_PATTERN.substring(0, MD5_PATTERN.length() - md5String.length()) + md5String;
        }
        catch(Throwable ignore) 
        {
            return null;
        }
        
        return md5String.toUpperCase();
    }
    
    public static class HMAC
    {
        private final static String HMAC_KEY_SPEC = "HmacSHA1";
        public static String encode(String secretKey, String plainText) 
        {
            byte[] bytes;
            try
            {
                SecretKeySpec key = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.US_ASCII.toString()), 
                    HMAC_KEY_SPEC);
                
                Mac mac = Mac.getInstance(HMAC_KEY_SPEC);
                mac.init(key);

                bytes = mac.doFinal(plainText.getBytes(StandardCharsets.US_ASCII));
            }
            catch(Throwable ignore)
            {
                return null;
            }
             
            return Base64.encodeBase64String(bytes);
        }
    }
    
    public static class AES256
    {
        public final static int SECRET_LENGTH = 32;
        public final static int IV_LENGTH = 16;
        
        private final static String ALGORITHM_NO_CBC = "AES";
        private final static String ALGORITHM_CBC = "AES/CBC/PKCS5PADDING";
        //private final static String ALGORITHM_CBC = "AES/CBC/NOPADDING";
        public static byte[] encrypt(
                SecretKey secretKey, 
                byte[] iv, 
                String plainText) 
        throws Exception
        {
            Cipher cipher = Cipher.getInstance(ALGORITHM_CBC); 
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));    
            byte[] byteCipherText = cipher.doFinal(
                plainText.getBytes(StandardCharsets.UTF_8));

            return byteCipherText;
        }
           
        public static String decrypt(
                SecretKey secretKey, 
                byte[] iv, 
                byte[] cipherText) 
        throws Exception
        {
            Cipher cipher = Cipher.getInstance(ALGORITHM_CBC); 
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));    
            byte[] decryptedText = cipher.doFinal(cipherText);
            return new String(decryptedText, StandardCharsets.UTF_8);
        }
        
        public static byte[] encrypt(
                SecretKey secretKey, 
                byte[] plainData) 
        throws Exception  
        {  
            Cipher cipher = Cipher.getInstance(ALGORITHM_NO_CBC);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey); 

            byte[] byteCipherText = cipher.doFinal(plainData);  
            return byteCipherText;  
        }  

        public static byte[] decrypt(
                SecretKey secretKey, 
                byte[] cipherData) 
        throws Exception  
        {  
            Cipher cipher = Cipher.getInstance(ALGORITHM_NO_CBC); 
            cipher.init(Cipher.DECRYPT_MODE, secretKey);  
                    
            byte[] decryptedText = cipher.doFinal(cipherData);  
            return decryptedText;  
        }  

    }
}
