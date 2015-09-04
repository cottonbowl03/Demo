package com.example.robert.demo;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
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
import java.net.URLConnection;

public class Demo extends AppCompatActivity {

    public String imageAddress;
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
                imageAddress = URLfield.getText().toString();
                if(imageAddress != null && URLUtil.isValidUrl(imageAddress)) {
                    setUpProgressDialog("Loading Image...");
                    alertView.setVisibility(View.GONE);
                    SaveNewFile s = new SaveNewFile();
                    s.execute(imageAddress);
                } else {
                    alertView.setVisibility(View.VISIBLE);
                    alertView.setText("Please enter a valid image URL.");
                }
            break;
        }
    }

    public Bitmap getBitmapFromLocation(String location) {
        File imgFile = new File(location);
        return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
    }

    public void clearClick(View v) {
        URLfield.setText("");
        URLfield.setHint("Please select an image");
        state = Options.NOTSUBMITTED;
        submit_but.setText("Load");
        submit_but.setEnabled(true);
        imageAddress = "http://www.online-image-editor.com//styles/2014/images/example_image.png";
        imagePreview.setImageResource(android.R.color.transparent);
        alertView.setVisibility(View.GONE);
    }

    public class SaveNewFile extends AsyncTask<String, Void, Boolean> {
        private boolean img;
        private HttpURLConnection connection;
        private boolean isSaveable;

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

            isSaveable = checkIfSaveable(imageAddress[0]);

            if (img && isSaveable) {
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
                submit_but.setText("Encrypt");
                state = Options.ENCRYPT;
            } else {
                alertView.setVisibility(View.VISIBLE);
                alertView.setText("Please enter a valid URL of an image. The image could be" +
                        " too large or not an image.");
            }
            dialog.hide();
        }

        private boolean checkIfSaveable(String address) {
            if(checkStorage()) {
                int file_size = 0;
                try {
                    URL url = new URL(address);
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.connect();
                    file_size = urlConnection.getContentLength();
                } catch (Exception e) {
                    //do something with the exception
                }
                return file_size < checkSizeOfStorage();
            }
            else {
                return false;
            }
        }

        private boolean checkStorage() {
            boolean mExternalStorageAvailable = false;
            boolean mExternalStorageWriteable = false;
            String state = Environment.getExternalStorageState();

            if (Environment.MEDIA_MOUNTED.equals(state)) {
                mExternalStorageAvailable = mExternalStorageWriteable = true;
            }
            return mExternalStorageAvailable && mExternalStorageWriteable;
        }

        private int checkSizeOfStorage() {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            //long bytesAvailable = (long)stat.getBlockSizeLong() *(long)stat.getAvailableBlocksLong(); //min api is 18
            long bytesAvailable = (long) stat.getBlockSize() * (long) stat.getBlockCount();
            return (int) bytesAvailable;
        }
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
}