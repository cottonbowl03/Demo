package com.example.robert.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Robert on 8/24/15.
 */
public class ImageEncryption {

    private String filePath;
    private byte[] comprImg;
    private byte[] key;
    private byte[] encryptedImage;
    private byte[] decryptedImage;

    public ImageEncryption(String filePath) {
        this.filePath = filePath;
    }

    public void encrypt() throws Exception{
        comprImg = imgCompress();
        key = createKey();
        encryptedImage = byteEncrypt(key, comprImg);
        Log.println(0,"filepath",filePath);
    }

    public void decrypt() throws Exception{
        decryptedImage = byteDecrypt(key, encryptedImage);
    }

    private byte[] imgCompress() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Bitmap image = getBitmapFromLocation(filePath);
        image.compress(Bitmap.CompressFormat.PNG, 100, baos); // bm is the bitmap object
        return baos.toByteArray();
    }

    private byte[] createKey() throws Exception {
        byte[] keyStart = "this is a key".getBytes();
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(keyStart);
        kgen.init(128, sr); // 192 and 256 bits may not be available
        SecretKey sKey = kgen.generateKey();
        return sKey.getEncoded();
    }

    private static byte[] byteEncrypt(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        return cipher.doFinal(clear);
    }

    public byte[] getEncryptedImage() {
        return encryptedImage;
    }

    private byte[] getDecryptedImage() {
        return decryptedImage;
    }

    public Bitmap retrieveFile() {
        byte[] DI = getDecryptedImage();
        return BitmapFactory.decodeByteArray(DI, 0, DI.length);
    }

    private static byte[] byteDecrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        return cipher.doFinal(encrypted);
    }

    private Bitmap getBitmapFromLocation(String location) {
        File imgFile = new File(location);
        return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
    }
}
