package com.vicky.cameraworking;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity  {

    // Activity request codes
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;
    Context context = null;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 123;
    // directory name to store captured images
    private static final String IMAGE_DIRECTORY_NAME = "Asisoft";
    public String filename="";

    private Uri fileUri; // file url to store image
    double latitude;
    double longitude;
    private ImageView imgPreview;
    private Button btnCapturePicture;
    private TextView imageDetails;
    private GoogleLocationService googleLocationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        imgPreview = (ImageView) findViewById(R.id.imgPreview);
        btnCapturePicture = (Button) findViewById(R.id.btnCapturePicture);
        imageDetails = (TextView) findViewById(R.id.image_Details);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getApplicationContext().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                // System.out.println("Permission Required Allow");
            } else {
                Log.d("Firstapp", "Already granted access");
                // System.out.println("Permission Already Allow");
            }
        }
        /**
         * Capture image button click event
         * */

        btnCapturePicture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // capture picture
                boolean result = Utils.checkPermission(MainActivity.this);
                if(result)
                    captureImage();
            }
        });

    }
    public void setFusedLatitude(double lat) {
        this.latitude = lat;
    }

    public void setFusedLongitude(double lon) {
        this.longitude = lon;
    }

    public double getFusedLatitude() {
        return this.latitude;
    }

    public double getFusedLongitude() {
        return this.longitude;
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        /*fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);*/

        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);

    }

    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /*
     * returning image / video
     */
    private static File getOutputMediaFile(int type) {

        // External sdcard location
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory().getPath(),
                IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(IMAGE_DIRECTORY_NAME, "Fisierul "
                        + IMAGE_DIRECTORY_NAME + " nu a fost creat");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFileName;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFileName = new File(mediaStorageDir.getPath() + File.separator
                    + "IMGGPS_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFileName;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                googleLocationService = new GoogleLocationService(context, new LocationUpdateListener() {
                    @Override
                    public void canReceiveLocationUpdates() {
                        System.out.println("***** Received ... ******");
                    }

                    @Override
                    public void cannotReceiveLocationUpdates() {
                        System.out.println("***** Not Recevied ... ******");
                    }

                    //update location to our servers for tracking purpose
                    @Override
                    public void updateLocation(Location location) {
                        if (location != null) {
                            setFusedLatitude(location.getLatitude());
                            setFusedLongitude(location.getLongitude());
                            System.out.println(location.getLatitude() + " ******* Updated Location *********** " + location.getLongitude());
                            previewCapturedImage(data, location.getLatitude(), location.getLongitude());
                            googleLocationService.stopLocationUpdates();
                        }
                        System.out.println("***** Not Found ... ******");
                    }

                    @Override
                    public void updateLocationName(String localityName, Location location) {

                        googleLocationService.stopLocationUpdates();
                    }
                });
                googleLocationService.startUpdates();
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture
                Toast.makeText(getApplicationContext(),
                        "Cancelled", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Error!", Toast.LENGTH_SHORT)
                        .show();
            }
        }

    }
    private void previewCapturedImage(Intent data,double latitude,double longitude){
        try {
            imgPreview.setVisibility(View.VISIBLE);
            filename = getFilename();
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
            FileOutputStream fo;
            try {
                fo = new FileOutputStream(filename);
                fo.write(bytes.toByteArray());
                fo.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                 e.printStackTrace();
            }
            System.out.println(filename);
            ExifInterface exif = new ExifInterface(filename);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPS.convert(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPS.latitudeRef(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPS.convert(longitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GPS.longitudeRef(longitude));
            exif.saveAttributes();
            imageDetails.setText(ReadExif(filename));
            imgPreview.setImageBitmap(thumbnail);
            //btnSend.setVisibility(View.VISIBLE);
        }
        catch (Exception exe1){
            exe1.printStackTrace();
        }
    }

    String ReadExif(String file){
        String exif="Exif: " + file;
        try {
            ExifInterface exifInterface = new ExifInterface(file);

            exif += "\nIMAGE_LENGTH: " + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            exif += "\nIMAGE_WIDTH: " + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            exif += "\n DATETIME: " + exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
            exif += "\n TAG_MAKE: " + exifInterface.getAttribute(ExifInterface.TAG_MAKE);
            exif += "\n TAG_MODEL: " + exifInterface.getAttribute(ExifInterface.TAG_MODEL);
            exif += "\n TAG_ORIENTATION: " + exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
            exif += "\n TAG_WHITE_BALANCE: " + exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
            exif += "\n TAG_FOCAL_LENGTH: " + exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
            exif += "\n TAG_FLASH: " + exifInterface.getAttribute(ExifInterface.TAG_FLASH);
            exif += "\nGPS related:";
            exif += "\n TAG_GPS_DATESTAMP: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
            exif += "\n TAG_GPS_TIMESTAMP: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
            exif += "\n TAG_GPS_LATITUDE: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            exif += "\n TAG_GPS_LATITUDE_REF: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            exif += "\n TAG_GPS_LONGITUDE: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            exif += "\n TAG_GPS_LONGITUDE_REF: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            exif += "\n TAG_GPS_PROCESSING_METHOD: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);

            Toast.makeText(MainActivity.this,
                    "finished",
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(MainActivity.this,
                    e.toString(),
                    Toast.LENGTH_LONG).show();
        }

        return exif;
    }
    private String getFilename() {

        String filepath = Environment.getExternalStorageDirectory().getPath();
        System.out.println("file path"+filepath);
        File file = new File(filepath, IMAGE_DIRECTORY_NAME);
        if (!file.exists()) {
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/GPS_Img_"+ new SimpleDateFormat("yyyy_MM_dd_HH_MM_ss").format(new Date())  + ".jpg");
    }

}
