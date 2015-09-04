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
 *
 * Class saves the file locally from a URL and returns the location on the disk
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
    //returns the locaiton on the local disk of the image
    public String getImageLocation() {
        downloadBitmapImage();
        return Environment.getExternalStorageDirectory().getPath()+"/"+pos+".png";
    }
    //opens up a URL connection and downloads the image
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