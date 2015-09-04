package com.example.robert.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

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

    public String getImageLocation() {
        downloadBitmapImage();
        String location = Environment.getExternalStorageDirectory().getPath()+"/"+pos+".png";
        return location;
    }

    void downloadBitmapImage() {
        InputStream input;
        try {
            URL url = new URL(strURL);
            input = url.openStream();
            byte[] buffer = new byte[500 * 1024]; //or 500*1024
            OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory().
                    getPath() + "/" + pos + ".png");
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
        catch (Exception e) {
                Log.e("MYAPP", "exception", e);
        }
    }
}