package com.example.robert.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
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

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.example.robert.demo.DownloadAndReadImage;

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
    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        URLfield = (EditText) findViewById(R.id.image_txtfield);
        imagePreview = (ImageView) findViewById(R.id.imagePreview);
        submit_but = (Button) findViewById(R.id.submit_but);
        submitted = false; //bool to check if user has inputted an image
        encrypted = false; //confirms that image has been encrypted
        count = 0;
    }

    public void submitClick(View v) throws Exception{
        if (!submitted) { submitFile(); }

        else { //image has already loaded --> time to encrypt and send

            if (!encrypted) {
                Picasso.with(this).load(lockImage).into(imagePreview);

                byte[] b = imgCompress();
                key = createKey();

                encryptedImage = byteEncrypt(key, b);
                encrypted = true;
                submit_but.setText("Retrieve");
            }
            else { //button shows retrieve
                byte[] decryptedData = byteDecrypt(key, encryptedImage); //error bc uninitialized key
                Bitmap retImage = retrieveFile(decryptedData);
                imagePreview.setImageBitmap(retImage);
                count++;
                //submit_but.setVisibility(View.GONE);
            }
        }
    }

    public void submitFile() {
        if(URLUtil.isValidUrl(imageAddress)) {
            new AsyncTask<Void, Void, Boolean>() {
                protected Boolean doInBackground(Void... voids) {
                    boolean img = false;
                    boolean youtube = false;
                    HttpURLConnection connection = null;
                    try {
                        URL url  = new URL(imageAddress);
                        connection = (HttpURLConnection)url.openConnection();
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
                    connection.disconnect();
                    return img || youtube;
                }

                protected void onPostExecute(Boolean valid) {
                    if (valid) {
                        Picasso.with(Demo.this).load(imageAddress).into(imagePreview);
                        saveImage();
                        //sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()))); //refreshes system to show saved file
                        submitted = true;
                        submit_but.setText("Encrypt");
                    }
                }
            }.execute();
        }
    }

    private void saveImage() {
        DownloadAndReadImage dImage = new DownloadAndReadImage(imageAddress, count);
//        dImage.downloadBitmapImage();
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
        imageAddress = "http://www.online-image-editor.com//styles/2014/images/example_image.png";
        imagePreview.setImageResource(android.R.color.transparent);
    }
}