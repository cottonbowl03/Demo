package com.example.robert.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Created by Robert on 8/20/15.
 */
public class DownloadAndReadImage {
    String strURL;
    int pos;
    Bitmap bitmap = null;

    // pass image url and Pos for example i:
    DownloadAndReadImage(String url,int position) {
        this.strURL = url;
        this.pos = position;
    }

    public Bitmap getBitmapImage() {
        downloadBitmapImage();
        return readBitmapImage();
    }

    public String getImageLocation() {
        return "/sdcard/"+pos+".png";
    }

    void downloadBitmapImage() {
        InputStream input;
        try {
            URL url = new URL (strURL);
            input = url.openStream();
            byte[] buffer = new byte[500*1024]; //or 500*1024
            OutputStream output = new FileOutputStream("/sdcard/"+pos+".png");
            try {
                int bytesRead = 0;
                while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                    output.write(buffer, 0, bytesRead);
                }
            }
            finally {
                output.close();
                input.close();
                buffer = null;
            }
        }
        catch(Exception e) {
            Log.e("MYAPP", "exception",e);
//            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    Bitmap readBitmapImage() {
        BitmapFactory.Options bOptions = new BitmapFactory.Options();
        bOptions.inTempStorage = new byte[16*1024*1024];

        String imageInSD = "/sdcard/"+pos+".png";

        bitmap = BitmapFactory.decodeFile(imageInSD,bOptions);
        boolean exists = new File(imageInSD).exists();
        return bitmap;
    }
}
