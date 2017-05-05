package com.spaceappsapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private String message;
    private String birdName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Intent intent = getIntent();
        String userMessage = intent.getStringExtra(GetDataActivity.EXTRA_MESSAGE);
        birdName = intent.getStringExtra(GetDataActivity.BIRD_NAME);

        if(userMessage.equals("debug")){

            this.message = "{\"data\":[";
            this.message += "{" +
                "\"timestamp\":\"123456789\"," +
                "\"lat\":\"45\"," +
                "\"long\":\"80\"" +
                "}";

            this.message += ",{" +
                "\"timestamp\":\"123456789\"," +
                "\"lat\":\"60\"," +
                "\"long\":\"-12\"" +
                "}";
            this.message += ",{" +
                "\"timestamp\":\"123456789\"," +
                "\"lat\":\"-70\"," +
                "\"long\":\"4\"" +
                "}";

            this.message += ",{" +
                "\"timestamp\":\"123456789\"," +
                "\"lat\":\"2\"," +
                "\"long\":\"55\"" +
                "}";

            this.message += ",{" +
                "\"timestamp\":\"123456789\"," +
                "\"lat\":\"-25\"," +
                "\"long\":\"55\"" +
                "}";

            this.message += ",{" +
                "\"timestamp\":\"123456789\"," +
                "\"lat\":\"65\"," +
                "\"long\":\"-56\"" +
                "}";

            this.message += ",{" +
                "\"timestamp\":\"123456789\"," +
                "\"lat\":\"45\"," +
                "\"long\":\"-75\"" +
                "}";

            this.message += "]}";
        } else {
            this.message = userMessage;
        }

        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            SupportMapFragment supportMapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
            supportMapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap map){
        mMap = map;

        try {
            JSONObject jmessage = new JSONObject(message);

            JSONArray jarray = jmessage.getJSONArray("data");

            if (mMap != null) {

                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter(){
                    @Override
                    public View getInfoWindow(Marker arg0){
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker){
                        LinearLayout info = new LinearLayout(getApplicationContext());
                        info.setOrientation(LinearLayout.VERTICAL);

                        TextView title = new TextView(getApplicationContext());
                        title.setTypeface(null, Typeface.BOLD);
                        title.setGravity(Gravity.CENTER);
                        title.setText(marker.getTitle());

                        TextView snippet = new TextView(getApplicationContext());
                        snippet.setTextColor(Color.BLACK);
                        //snippet.setText(marker.getSnippet());


                        String[] splitSnippet = marker.getSnippet().split("\n");
                        snippet.setText(splitSnippet[0]+"\n"+splitSnippet[1]+"\n"+splitSnippet[2]);

                        info.addView(title);
                        info.addView(snippet);

                        ImageView imageView = new ImageView(getApplicationContext());

                        HttpURLConnection urlConnection = null;

                        //Hack way to solve this problem :)
                        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                        StrictMode.setThreadPolicy(policy);
                        try {
                            String pictureUrl = splitSnippet[3];
                            Log.d("url", pictureUrl);
                            URL url = new URL(pictureUrl);
                            urlConnection = (HttpURLConnection) url.openConnection();
                            Bitmap bmp = BitmapFactory.decodeStream(urlConnection.getInputStream());

                            double ratio = ((double) bmp.getHeight())/450;

                            Bitmap resized = Bitmap.createScaledBitmap(bmp, (int) Math.ceil(bmp.getWidth() / ratio), (int) Math.ceil(bmp.getHeight() / ratio), true);
                            imageView.setImageBitmap(resized);
                            info.addView(imageView);
                        }
                        catch(java.io.IOException e){
                            Log.e("error", e.getMessage());
                        }
                        finally{
                            if (urlConnection != null){
                                urlConnection.disconnect();
                            }
                        }

                        return info;
                    }

                });

                Double sumLat = 0.0;
                Double sumLong = 0.0;
                for (int i = 0; i < jarray.length(); i++) {
                    Double latitude = jarray.getJSONObject(i).getDouble("lat");
                    Double longitude =jarray.getJSONObject(i).getDouble("long");

                    sumLat += latitude;
                    sumLong += longitude;

                    LatLng latLng = new LatLng(latitude, longitude);
                    //mMap.addMarker(new MarkerOptions().position(latLng).title(birdName).snippet("Time: " + jarray.getJSONObject(i).getString("timestamp") + "\nLat: " + latitude + "\nLong: " + longitude));

                    mMap.addMarker(new MarkerOptions().position(latLng).title(jarray.getJSONObject(i).getString("name")).snippet("Time: " + jarray.getJSONObject(i).getString("timestamp") + "\nLat: " + latitude + "\nLong: " + longitude + "\n" + jarray.getJSONObject(i).getString("image")));
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(sumLat / jarray.length(), sumLong / jarray.length())));

            }
        } catch (org.json.JSONException e) {
            Log.e("Error", "JSON Exception: Message: "+message);
            return;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_maps, menu);
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

}
