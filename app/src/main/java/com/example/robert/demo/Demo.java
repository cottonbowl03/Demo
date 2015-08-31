package com.example.robert.demo;

import android.app.ProgressDialog;
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

import java.io.File;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;

import com.parse.Parse;

public class Demo extends AppCompatActivity {

    public String imageAddress = "http://www.jpl.nasa.gov/spaceimages/images/mediumsize/PIA17011_ip.jpg";
    private EditText URLfield;
    private ImageView imagePreview;
    private Button submit_but;
    private boolean submitted;
    private boolean encrypted;
    private boolean sent;
    public String lockImage = "http://vignette3.wikia.nocookie.net/cityofwonder/images/9/96/Lock.png/revision/latest?cb=20110125080244";
    public String sentImage = "http://simpleicon.com/wp-content/uploads/sent-mail-3.png";
    private String imageLocation;
    public String media;
    private int count;
    private ImageEncryption IE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        URLfield = (EditText) findViewById(R.id.image_txtfield);
        imagePreview = (ImageView) findViewById(R.id.imagePreview);
        submit_but = (Button) findViewById(R.id.submit_but);
        submitted = false; //bool to check if user has inputted an image
        encrypted = false; //confirms that image has been encrypted
        sent = false;
        count = 0;

        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "iJSgVrh696NKtTrM3zvwNsXbXLRMgtTk2hQb8sFN", "CzGzENNv8zSjRnDXuyE5eRf98I3q6O5YWDhUSlf3");

//        ParseObject testObject = new ParseObject("TestObject");
//        testObject.put("foo", "bar");
//        testObject.saveInBackground();
    }

    public void submitClick(View v) throws Exception{
        if (!submitted) {
            URLfield.setText(imageAddress);
            URLfield.setKeyListener(null);
            if(URLUtil.isValidUrl(imageAddress)) {
                SaveNewFile s = new SaveNewFile();
                s.execute(imageAddress);
            }
        }

        else if (!encrypted) {
            IE = new ImageEncryption(imageLocation);
            IE.encrypt();
            Picasso.with(this).load(lockImage).into(imagePreview);
            encrypted = true;
            submit_but.setText("Send");
        }

        else if(!sent) {
            ImageToServer fileToSend = new ImageToServer(IE.getEncryptedImage());
            fileToSend.sendImage();
            Picasso.with(this).load(sentImage).into(imagePreview);
            sent = true;
            submit_but.setText("Retrieve");
        }

        else { //button shows retrieve
            IE.decrypt();
            Bitmap retImage = IE.retrieveFile();
            imagePreview.setImageBitmap(retImage);
            count++;
            submit_but.setText("Load");
            submit_but.setEnabled(false);
        }
    }

    public Bitmap getBitmapFromLocation(String location) {
        File imgFile = new File(location);
        return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
    }

    public void clearClick(View v) {
        URLfield.setText("Please select an image");
        submitted = false;
        encrypted = false;
        sent = false;
        submit_but.setText("Load");
        submit_but.setEnabled(true);
        imageAddress = "http://www.online-image-editor.com//styles/2014/images/example_image.png";
        imagePreview.setImageResource(android.R.color.transparent);
    }

    public class SaveNewFile extends AsyncTask<String, Void, Boolean>{
        private boolean img;
        private boolean youtube;
        private HttpURLConnection connection;
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(Demo.this);
            dialog.setMessage("Loading content...");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setProgress(0);
            dialog.setMax(100);
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(String... imageAddress) {
            img = false;
            youtube = false;
            connection = null;
            try {
                URL url = new URL(imageAddress[0]);
                connection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dialog.incrementProgressBy(20);
            String contentType = connection.getHeaderField("Content-Type");
            img = contentType.startsWith("image/");
            if (img)
                media = "image";
            if (!img) {
                // Check host of url if youtube exists
                Uri uri = Uri.parse(imageAddress[0]);
                if ("www.youtube.com".equals(uri.getHost())) {
                    media = "youtube";
                    youtube = true;
                }
            }
            dialog.incrementProgressBy(30);
            connection.disconnect();
            if(img || youtube) {
                DownloadAndReadImage dImage = new DownloadAndReadImage(imageAddress[0], count);
                //image = dImage.getBitmapImage();
                dialog.incrementProgressBy(30);
                imageLocation = dImage.getImageLocation();
            }
            dialog.incrementProgressBy(20);
            return img||youtube;
        }

        @Override
        protected void onPostExecute(Boolean valid){
            submitted = true;
            imagePreview.setImageBitmap(getBitmapFromLocation(imageLocation));
            submit_but.setText("Encrypt");
            dialog.hide();
        }
    }
}