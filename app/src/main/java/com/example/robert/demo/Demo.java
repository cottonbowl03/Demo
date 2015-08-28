package com.example.robert.demo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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

public class Demo extends AppCompatActivity {

    public String imageAddress =
            "http://globe-views.com/dcim/dreams/image/image-04.jpg";
    private EditText URLfield;
    private ImageView imagePreview;
    private Button submit_but;
    private boolean submitted;
    private boolean encrypted;
    private boolean sent;
    final public String lockImage =
            "http://vignette3.wikia.nocookie.net/cityofwonder/images/9/96/Lock.png/revision/latest?cb=20110125080244";
    final public String sentImage =
            "http://simpleicon.com/wp-content/uploads/sent-mail-3.png";
    private String imageLocation;
    public String media;
    private int count;
    private ImageEncryption IE;
    private AlertDialog alert1;

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
        createAlert();
    }

    public void createAlert() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Please enter a valid URL");
        builder1.setCancelable(true);
        builder1.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        builder1.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        alert1 = builder1.create();
    }

    public void submitClick(View v) throws Exception{
        if (!submitted) {
            URLfield.setText(imageAddress);
            URLfield.setKeyListener(null);
            //imageAddress = URLfield.getText().toString();    //want to keep this!
            if(URLUtil.isValidUrl(imageAddress)) {
                SaveNewFile s = new SaveNewFile();
                s.execute(imageAddress);
            }
            else {
                alert1.show();
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
        //URLfield.setText("Please select an image");
        submitted = false;
        encrypted = false;
        sent = false;
        submit_but.setText("Load");
        submit_but.setEnabled(true);
        imageAddress = "http://www.online-image-editor.com//styles/2014/images/example_image.png";
        URLfield.setText(null);
        URLfield.setHint("Please enter an image or video address");
        imagePreview.setImageResource(android.R.color.transparent);
    }

    public class SaveNewFile extends AsyncTask<String, Integer, Boolean>{
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
                dialog.incrementProgressBy(30);
                //image = dImage.getBitmapImage();
                imageLocation = dImage.getImageLocation();
            }
            else {
                alert1.show();
            }
            dialog.incrementProgressBy(20);
            return img||youtube;
        }

        @Override
        protected void onPostExecute(Boolean valid){
            submitted = true;
            imagePreview.setImageBitmap(getBitmapFromLocation(imageLocation));

            submit_but.setText("Encrypt");
            //dialog.hide();
        }
    }
}