package com.example.robert.demo;

import android.os.AsyncTask;
import android.os.Environment;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Robert on 8/24/15.
 */
public class ImageToServer {

    public String serverAddress = "robertwaynejr.bugs3.com";
    public byte[] encryptedData;
    public String fileName = "ServerFile";

    public ImageToServer (byte[] encryptedData) {
        this.encryptedData = encryptedData;
    }

    public void sendImage() throws IOException {
        saveImage();
        AsyncHttpPostTask task = new AsyncHttpPostTask(serverAddress);
        task.execute(fileName);
    }

    public void saveImage() throws IOException {
        File root = Environment.getExternalStorageDirectory();
        File imgFile = new File(root, "fileName");
        FileOutputStream fos = new FileOutputStream(imgFile);
        fos.write(encryptedData);
        fos.close();
    }

    public class AsyncHttpPostTask extends AsyncTask<String, Void, String> {

        private String server;

        public AsyncHttpPostTask(final String server) {
            this.server = server;
        }

        @Override
        protected String doInBackground(String... params) {
            //String url = "robertwaynejr.bugs3.com";
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), params[0]);
            try {
                HttpClient httpclient = new DefaultHttpClient();

                HttpPost httppost = new HttpPost(server);

                InputStreamEntity reqEntity = new InputStreamEntity(new FileInputStream(file), -1);
                reqEntity.setContentType("binary/octet-stream");
                reqEntity.setChunked(true); // Send in multiple parts if needed
                httppost.setEntity(reqEntity);
                HttpResponse response = httpclient.execute(httppost);
                //Do something with response...
            } catch (Exception e) {
                e.printStackTrace();
            }
        return null;
        }
    }
}