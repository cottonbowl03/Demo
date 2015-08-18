package com.example.robert.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.squareup.picasso.Picasso;

import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Demo extends AppCompatActivity {

    public String imageAddress = "http://www.jpl.nasa.gov/spaceimages/images/mediumsize/PIA17011_ip.jpg";
    private EditText URLfield;
    private ImageView imagePreview;
    private Button submit_but;
    private boolean submitted;
    private boolean encrypted;
    public String lockImage = "http://vignette3.wikia.nocookie.net/cityofwonder/images/9/96/Lock.png/revision/latest?cb=20110125080244";
    private Bitmap image;
    private byte[] key;
    private byte[] encryptedImage;
    private boolean valid;
    private String media;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        URLfield = (EditText) findViewById(R.id.image_txtfield);
        imagePreview = (ImageView) findViewById(R.id.imagePreview);
        submit_but = (Button) findViewById(R.id.submit_but);
        submitted = false; //bool to check if user has inputted an image
        encrypted = false; //confirms that image has been encrypted
    }

    public void submitClick(View v) throws Exception{
        if (!submitted) { submitFile(); }

        else { //image has already loaded --> time to encrypt and send

            if (!encrypted) {
                Picasso.with(this).load(lockImage).into(imagePreview);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.PNG, 100, baos); // bm is the bitmap object
                byte[] b = baos.toByteArray();

                byte[] keyStart = "this is a key".getBytes();
                KeyGenerator kgen = KeyGenerator.getInstance("AES");
                SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
                sr.setSeed(keyStart);
                kgen.init(128, sr); // 192 and 256 bits may not be available
                SecretKey sKey = kgen.generateKey();
                key = sKey.getEncoded();

                encryptedImage = byteEncrypt(key, b);
                encrypted = true;
                submit_but.setText("Retrieve");
            }
            else { //button shows retrieve
                byte[] decryptedData = byteDecrypt(key, encryptedImage); //error bc uninitialized key
                Bitmap retImage = retrieveFile(decryptedData);
                imagePreview.setImageBitmap(retImage);
            }
        }
    }

    public void submitFile() {
        checkContent();
        if (valid) {
            Picasso.with(this).load(imageAddress).into(imagePreview);
            saveImage();
            //sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()))); //refreshes system to show saved file
            submitted = true;
            submit_but.setText("Encrypt");
        }
    }

    private void checkContent() {
        media = null;
        if(URLUtil.isValidUrl(imageAddress)) {
            Thread thread = new Thread() {
                boolean img = false;
                boolean youtube = false;
                public void run() {
                    URLConnection connection = null;
                    try {
                        connection = new URL(imageAddress).openConnection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String contentType = connection.getHeaderField("Content-Type");
                    img = contentType.startsWith("image/");
                    if(img)
                        media = "image";
                    if (!img) {
                        // Check host of url if youtube exists
                        Uri uri = Uri.parse(imageAddress);
                        if ("www.youtube.com".equals(uri.getHost())) {
                            media = "youtube";
                            youtube = true;
                        }
                    }
                    valid = img || youtube;
                }
            };
            thread.start();
        }
    }


    private void saveImage() {
        DownloadAndReadImage dImage= new DownloadAndReadImage(imageAddress,0);
        dImage.downloadBitmapImage();
        image = dImage.getBitmapImage();
    }

    private byte[] imgCompress() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
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

    private static byte[] byteDecrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        return cipher.doFinal(encrypted);
    }

    public Bitmap retrieveFile(byte[] byteUnencrypted) { //unencrypt and display file
        return BitmapFactory.decodeByteArray(byteUnencrypted , 0, byteUnencrypted.length);
    }

    public void clearClick(View v) {
        URLfield.setText("Please select an image");
        submitted = false;
        encrypted = false;
        submit_but.setText("Load");
        imagePreview.setImageResource(android.R.color.transparent);
    }

    public class DownloadAndReadImage {
        String strURL;
        int pos;
        Bitmap bitmap = null;

        // pass image url and Pos for example i:
        DownloadAndReadImage(String url,int position) {
            this.strURL = url;
            this.pos = position;
        }

        public String getBitmapLocation() {
            return "/sdcard/"+pos+".png";
        }

        public Bitmap getBitmapImage() {
            downloadBitmapImage();
            return readBitmapImage();
        }

        void downloadBitmapImage() {
            InputStream input;
            try {
                URL url = new URL (strURL);
                input = url.openStream();
                byte[] buffer = new byte[1500]; //or 500*1024
                OutputStream output = new FileOutputStream ("/sdcard/"+pos+".png");
                try {
                    int bytesRead = 0;
                    while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                        output.write(buffer, 0, bytesRead);
                    }
                }
                finally {
                    output.close();
                    buffer = null;
                }
            }
            catch(Exception e) {
                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            }
        }

        Bitmap readBitmapImage() {
            BitmapFactory.Options bOptions = new BitmapFactory.Options();
            bOptions.inTempStorage = new byte[16*1024*1024];

            String imageInSD = "/sdcard/"+pos+".png";

            bitmap = BitmapFactory.decodeFile(imageInSD,bOptions);
            return bitmap;
        }
    }
}
