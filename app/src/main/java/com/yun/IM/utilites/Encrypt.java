package com.yun.IM.utilites;

import static com.yun.IM.utilites.Constants.DEFAULT_CIPHER_ALGORITHM;
import static com.yun.IM.utilites.Constants.KEY_ALGORITHM;

import android.util.Base64;

import com.yun.IM.models.Message;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Encrypt {
    /**
     * AES 加密操作
     *
     * @param message 待加密内容
     * @param key     加密密钥
     * @return 返回Base64转码后的加密数据
     */
    public static String encrypt(Message message, String key) {
        if (key != null && !key.equals("")) {
            try {
                Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
                byte[] byteContent = message.message.getBytes("utf-8");
                cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(key));
                byte[] result = cipher.doFinal(byteContent);
                return byte2Base64(result);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return message.message;
    }

    /**
     * AES 解密操作
     *
     * @param content
     * @param key
     * @return
     */
    public static String decrypt(String content, String key) {
        if (key != null && !key.equals("")) {
            try {
                Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(key));
                byte[] result = cipher.doFinal(base642Byte(content));
                String value= new String(result,"utf-8");
                return value;
            } catch (Exception ex) {
                return content;
            }
        }
        return content;
    }

    /**
     * 生成加密秘钥
     *
     * @return
     */
    private static SecretKeySpec getSecretKey(final String key) {
        try {
            byte[] keyBytes = key.getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hashedKey = sha.digest(keyBytes);
            return new SecretKeySpec(hashedKey, KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * 字节数组转Base64编码
     *
     * @param bytes
     * @return
     */
    private static String byte2Base64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    /**
     * Base64编码转字节数组
     *
     * @param base64Key
     * @return
     * @throws IOException
     */
    private static byte[] base642Byte(String base64Key) {
        return Base64.decode(base64Key, Base64.DEFAULT);
    }

    /**
     * md5加密操作
     *
     * @param message
     * @return
     */
    public static String returnMd5Message(String message) {
        StringBuffer sb = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(message.getBytes());
            byte[] digest = md.digest();
            sb = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100).substring(1, 3));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
