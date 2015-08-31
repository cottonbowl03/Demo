package com.example.robert.demo;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.parse.Parse;
import com.parse.GetDataCallback;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseException;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;




public class Demo extends AppCompatActivity {

    public String imageAddress = "http://www.jpl.nasa.gov/spaceimages/images/mediumsize/PIA17011_ip.jpg";
    private EditText URLfield;
    private ImageView imagePreview;
    private Button submit_but;
    private boolean submitted;
    private boolean encrypted;
    private boolean sent;

    private enum Options {NOTSUBMITTED, ENCRYPT, SEND, RETRIEVE}
    private Options state;

    public String lockImage = "http://vignette3.wikia.nocookie.net/cityofwonder/images/9/96/Lock.png/revision/latest?cb=20110125080244";
    public String sentImage = "http://simpleicon.com/wp-content/uploads/sent-mail-3.png";
    private String imageLocation;
    public String media;
    private int count;
    private ImageEncryption IE;
    ParseObject encryptedImageObj;
    public ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        URLfield = (EditText) findViewById(R.id.image_txtfield);
        imagePreview = (ImageView) findViewById(R.id.imagePreview);
        submit_but = (Button) findViewById(R.id.submit_but);
        state = Options.NOTSUBMITTED;
        count = 0;

        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.setMax(100);

        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "iJSgVrh696NKtTrM3zvwNsXbXLRMgtTk2hQb8sFN", "CzGzENNv8zSjRnDXuyE5eRf98I3q6O5YWDhUSlf3");
    }

    public void submitClick(View v) throws Exception{

        switch(state) {
            case ENCRYPT:
                dialog.setMessage("Encrypting image...");
                dialog.setProgress(0);
                dialog.show();

                IE = new ImageEncryption(imageLocation);
                dialog.incrementProgressBy(20);
                IE.encrypt(dialog);
                dialog.incrementProgressBy(20);
                Picasso.with(this).load(lockImage).into(imagePreview);
                dialog.hide();

                state = Options.SEND;
                submit_but.setText("Send");
                break;

            case SEND:
                dialog.setMessage("Sending image...");
                dialog.setProgress(0);
                dialog.show();

                byte[] encryptedImageBytes = IE.getEncryptedImage();
                dialog.incrementProgressBy(30);
                ParseFile file = new ParseFile("img.png", encryptedImageBytes); //must save the file type of the file ".png"
                file.saveInBackground();
                dialog.incrementProgressBy(20);

                encryptedImageObj = new ParseObject("EncryptedImage");
                encryptedImageObj.put("encryptedFile", file);
                dialog.incrementProgressBy(20);
                encryptedImageObj.saveInBackground();

                Picasso.with(this).load(sentImage).into(imagePreview);
                sent = true;
                dialog.incrementProgressBy(30);
                dialog.hide();

                state = Options.RETRIEVE;
                submit_but.setText("Retrieve");
                break;

            case RETRIEVE: //button shows retrieve
                dialog.setMessage("Retrieving image...");
                dialog.setProgress(0);
                dialog.show();

                ParseFile encryptedImageRetrieve = (ParseFile) encryptedImageObj.get("encryptedFile");
                dialog.incrementProgressBy(30);
                encryptedImageRetrieve.getDataInBackground(new GetDataCallback() {
                    public void done(byte[] data, ParseException e) {
                        if (e == null) {
                            IE.updatedEncrytedImage(data);
                            dialog.incrementProgressBy(30);
                        } else {
                            e.printStackTrace();
                        }
                    }
                });
                IE.decrypt();
                dialog.incrementProgressBy(30);
                Bitmap retImage = IE.retrieveFile();
                imagePreview.setImageBitmap(retImage);
                count++;
                dialog.incrementProgressBy(10);
                dialog.hide();

                state = Options.NOTSUBMITTED;
                submit_but.setText("Load");
                submit_but.setEnabled(false);
                break;

            default: //not yet loaded
                URLfield.setText(imageAddress);
                URLfield.setKeyListener(null);
                if (URLUtil.isValidUrl(imageAddress)) {
                    SaveNewFile s = new SaveNewFile();
                    s.execute(imageAddress);
                }
                state = Options.ENCRYPT;
                break;
        }
    }


    public Bitmap getBitmapFromLocation(String location) {
        File imgFile = new File(location);
        return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
    }

    public void clearClick(View v) {
        URLfield.setText("Please select an image");
        state = Options.NOTSUBMITTED;
        submit_but.setText("Load");
        submit_but.setEnabled(true);
        imageAddress = "http://www.online-image-editor.com//styles/2014/images/example_image.png";
        imagePreview.setImageResource(android.R.color.transparent);
    }

    public class SaveNewFile extends AsyncTask<String, Void, Boolean>{
        private boolean img;
        private boolean youtube;
        private HttpURLConnection connection;

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Loading content...");
            dialog.setProgress(0);
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
            } dialog.incrementProgressBy(20);

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
            } dialog.incrementProgressBy(30);
            connection.disconnect();

            if(img || youtube) {
                DownloadAndReadImage dImage = new DownloadAndReadImage(imageAddress[0], count);
                dialog.incrementProgressBy(30);
                imageLocation = dImage.getImageLocation();
            } dialog.incrementProgressBy(50);
            return img||youtube;
        }

        @Override
        protected void onPostExecute(Boolean valid){
            imagePreview.setImageBitmap(getBitmapFromLocation(imageLocation));
            dialog.hide();
        }
    }
}