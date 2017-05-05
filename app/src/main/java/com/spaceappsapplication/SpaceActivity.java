package com.spaceappsapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;

public class SpaceActivity extends Activity {

    protected int MY_PERMISSION_ACCESS_COURSE_LOCATION = 1;
    protected int MY_PERMISSION_ACCESS_FINE_LOCATION = 2;
    protected int MY_PERMISSION_INTERNET = 3;
    protected static String urlPost = null; //Insert correct url
    protected static String urlGet = null; //Insert correct url

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap imageBitmap;
    private Double latitude;
    private Double longitude;
    private RequestAsyncTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_space);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_ACCESS_COURSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_FINE_LOCATION  }, MY_PERMISSION_ACCESS_FINE_LOCATION );
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.INTERNET  }, MY_PERMISSION_INTERNET );
        }

        gpsView = (TextView) this.findViewById(R.id.gps);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        LocationService locServe = new LocationService();

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locServe);
            Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if(loc != null) {
                latitude = loc.getLatitude();
                longitude = loc.getLongitude();
            }

        } catch (SecurityException e){
            Log.e("Error","GPS Permission Error");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_space, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /*
    public void logData(View view){
        Intent intent = new Intent(this, TakePictureActivity.class);
        startActivity(intent);
    }
    */
    public void viewData(View view){
        //Intent intent = new Intent(this,MapsActivity.class);
        //intent.putExtra(GetDataActivity.EXTRA_MESSAGE,"debug");
        //intent.putExtra(GetDataActivity.BIRD_NAME,"Eagle");
        Intent intent = new Intent(this,GetDataActivity.class);
        startActivity(intent);
    }

    public void logData(View view){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            if (imageBitmap == null){
                Toast.makeText(
                        getBaseContext(),
                        "No Image Stored: Data Not Sent!", Toast.LENGTH_SHORT).show();
                return;
            }
            if(longitude == null || latitude == null){

                Toast.makeText(
                        getBaseContext(),
                        "No GPS Coordinates: Data Not Sent!", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArray);
            String image = Base64.encodeToString(byteArray.toByteArray(), Base64.DEFAULT);

            String time = new Timestamp(System.currentTimeMillis()).toString();
            JSONObject msg = new JSONObject();
            try {


                JSONObject gps = new JSONObject();
                gps.put("lat", latitude);
                gps.put("long", longitude);

                msg.put("timestamp", time);
                msg.put("gps", gps);
                msg.put("image", image);
                task = new RequestAsyncTask( SpaceActivity.this, msg.toString(), SpaceActivity.urlPost);
                task.execute();

            } catch (org.json.JSONException e) {
                Log.e("Error",e.getMessage());
                Log.d("Message", msg.toString());
            }
        }
    }

    private class LocationService implements LocationListener {

        @Override
        public void onLocationChanged(Location loc){
            if(loc != null) {
                latitude = loc.getLatitude();
                longitude = loc.getLongitude();
            }
        }
        @Override
        public void onProviderDisabled(String provider){}
        @Override
        public void onProviderEnabled(String provider){}
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras){}

    }

    public String sendRequest(String message, String website){
        String serverResponse = "Error";
        HttpURLConnection urlConnection = null;
        BufferedReader inReader = null;
        try {
            URL url = new URL(website);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);

            urlConnection.setFixedLengthStreamingMode(message.getBytes().length);
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            urlConnection.connect();


            OutputStream os = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

            Log.d("Test This Part", message);

            writer.write(message);
            writer.flush();
            writer.close();
            os.close();
            Integer statusCode = urlConnection.getResponseCode();

            inReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

            String data;
            serverResponse = "";
            Log.d("Status", statusCode.toString());
            while ((data = inReader.readLine()) != null){
                serverResponse += data + "\n";
            }
        } catch (IOException e) {
            serverResponse = e.getMessage();
            e.printStackTrace();
        } finally {
            if (urlConnection != null){
                urlConnection.disconnect();
            }
            if (inReader != null){
                try {
                    inReader.close();
                }
                catch (IOException e){
                    // IO error
                    serverResponse = e.getMessage();
                    e.printStackTrace();
                }
            }
        }
        return serverResponse;
    }

    private class RequestAsyncTask extends AsyncTask<Void, Void, Void> {

        private String requestReply, website;
        private String message;
        private Context context;
        private AlertDialog alertDialog;

        public RequestAsyncTask(Context context, String message, String website){
            this.message = message;
            this.website = website;
            this.context = context;
            alertDialog = new AlertDialog.Builder(context)
                    .setTitle("Response From Server:")
                    .setCancelable(true)
                    .create();
        }

        @Override
        protected Void doInBackground(Void... voids){
            alertDialog.setMessage("Data sent, waiting for reply from server...");
            if(!alertDialog.isShowing())
            {
                alertDialog.show();
            }
            requestReply = sendRequest(message, website);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid){

            try {

                JSONObject obj = new JSONObject(requestReply);

                String jsonMsg = "Top match:\n   "+obj.getJSONArray("top1").getString(1)+": "+ obj.getJSONArray("top1").getString(0);
                jsonMsg += "\n\nOther Matches:\n";
                jsonMsg += "   "+obj.getJSONArray("top3").getJSONArray(1).getString(1)+": "+obj.getJSONArray("top3").getJSONArray(1).getString(0)+"\n";
                jsonMsg += "   "+obj.getJSONArray("top3").getJSONArray(2).getString(1)+": "+obj.getJSONArray("top3").getJSONArray(2).getString(0);
                alertDialog.setMessage("Reply: \n" + jsonMsg);
            } catch (org.json.JSONException e) {
                Log.e("Error", "JSON Exception: Message: " + requestReply);
                alertDialog.setMessage("Reply: " + requestReply);
            }

            if(!alertDialog.isShowing())
            {
                alertDialog.show();
            }
        }

        @Override
        protected void onPreExecute(){
            alertDialog.setMessage("Sending Message");
            if(!alertDialog.isShowing())
            {
                alertDialog.show();
            }
        }
    }

}
