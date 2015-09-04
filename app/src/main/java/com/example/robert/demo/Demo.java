package com.example.robert.demo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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

    public String imageAddress = "http://www.jpl.nasa.gov/spaceimages/images/" +
            "mediumsize/PIA17011_ip.jpg";
    //public String imageAddress = "https://developer.android.com/tools/support-library/features.html";
    private EditText URLfield;
    private TextView alertView;
    private ImageView imagePreview;
    private Button submit_but;
    private Button clear_but;

    private enum Options {NOTSUBMITTED, ENCRYPT, SEND, RETRIEVE}
    private Options state;

    public String lockImage = "http://vignette3.wikia.nocookie.net/cityofwonder/images/9/96/" +
            "Lock.png/revision/latest?cb=20110125080244";
    public String sentImage = "http://simpleicon.com/wp-content/uploads/sent-mail-3.png";
    private String imageLocation;
    public String media;  //<====?????
    private int count;
    private ImageEncryption IE;
    private ParseObject encryptedImageObj;
    public ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        URLfield = (EditText) findViewById(R.id.image_txtfield);
        imagePreview = (ImageView) findViewById(R.id.imagePreview);
        submit_but = (Button) findViewById(R.id.submit_but);
        clear_but = (Button) findViewById(R.id.clear_but);
        alertView = (TextView) findViewById(R.id.alert_view);
        state = Options.NOTSUBMITTED;
        count = 0;

        createProgressDialog();

        isNetworkAvailable();

        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "iJSgVrh696NKtTrM3zvwNsXbXLRMgtTk2hQb8sFN",
                "CzGzENNv8zSjRnDXuyE5eRf98I3q6O5YWDhUSlf3");
    }

    public void submitClick(View v) throws Exception{
        alertView.setVisibility(View.GONE);
        isNetworkAvailable();
        switch(state) {

            case ENCRYPT:
                setUpProgressDialog("Encrypting Image...");

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
                setUpProgressDialog("Sending Image...");

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
                dialog.incrementProgressBy(30);
                dialog.hide();

                state = Options.RETRIEVE;
                submit_but.setText("Retrieve");
                break;

            case RETRIEVE: //button shows retrieve
                setUpProgressDialog("Retrieving image...");

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

            default: //not yet loaded; Options.NOTLOADED
                setUpProgressDialog("Loading Image...");
                alertView.setVisibility(View.GONE);
                URLfield.setText(imageAddress);
                URLfield.setKeyListener(null);

                if (URLUtil.isValidUrl(imageAddress)) {
                    SaveNewFile s = new SaveNewFile();
                    s.execute(imageAddress);
                }

                if (imagePreview.getDrawable()==null) {
                    alertView.setVisibility(View.VISIBLE);
                    alertView.setText("Please enter a valid URL of an image.");
                }
                else {
                    submit_but.setText("Encrypt");
                    state = Options.ENCRYPT;
                    break;
                }
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
        //imageAddress = "http://www.online-image-editor.com//styles/2014/images/example_image.png";
        imageAddress = "https://developer.android.com/tools/support-library/features.html";
        imagePreview.setImageResource(android.R.color.transparent);
        alertView.setVisibility(View.GONE);
    }

    public class SaveNewFile extends AsyncTask<String, Void, Boolean> {
        private boolean img;
        private HttpURLConnection connection;
        private boolean result;

        @Override
        protected Boolean doInBackground(String... imageAddress) {
            img = false;
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

            dialog.incrementProgressBy(30);
            connection.disconnect();

            if (img) {
                DownloadAndReadImage dImage = new DownloadAndReadImage(imageAddress[0], count);
                dialog.incrementProgressBy(30);
                imageLocation = dImage.getImageLocation();
                dialog.incrementProgressBy(50);
                return img;
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean valid) {
            if (valid) {
                imagePreview.setImageBitmap(getBitmapFromLocation(imageLocation));
                //result = true;
            }
            else {
                //result = false;
                //showErrorDialog();
            }
            dialog.hide();
        }

        private boolean getResult() { return img; }
    }

    private void isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean connected = (activeNetworkInfo != null && activeNetworkInfo.isConnected());
        if (!connected) { //if there is no network connection
            alertView.setVisibility(View.VISIBLE);
            alertView.setText("No network detected. Please connect to a network.");
            URLfield.setEnabled(false);
            clear_but.setEnabled(false);
            submit_but.setEnabled(false);
            isNetworkAvailable();
        }
        else {
            alertView.setVisibility(View.GONE);
            URLfield.setEnabled(true);
            clear_but.setEnabled(true);
            submit_but.setEnabled(true);
        }
    }

    private void createProgressDialog() {
        dialog = new ProgressDialog(Demo.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.setMax(100);
        dialog.setCancelable(false);
    }

    private void setUpProgressDialog(String text) {
        dialog.setMessage(text);
        dialog.setProgress(0);
        dialog.show();
    }
//    private void showErrorDialog() {
//        new AlertDialog.Builder(Demo.this)
//                .setTitle("Issue with URL")
//                .setMessage("The URL you provided is not a supported image. Please " +
//                        "try a different URL.")
//                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        clear_but.performClick();
//                    }
//                })
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .setCancelable(false)
//                .show();
//    }
}