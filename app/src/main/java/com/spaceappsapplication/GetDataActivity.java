package com.spaceappsapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class GetDataActivity extends Activity {

    public final static String EXTRA_MESSAGE = "com.spaceappsapplication.message";
    public final static String BIRD_NAME = "com.spaceappsapplication.birdname";
    private String birdName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data);

        String[] arr = {
            "Robin","Jay","Falcon","Eagle","Seagull","Tweety bird","Hooded Warbler", "Hooded Oriol", "Indigo Bunting"
        };

        AutoCompleteTextView actv = (AutoCompleteTextView) findViewById(R.id.birdName);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, arr);
        actv.setThreshold(0);
        actv.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_get_data, menu);
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

    public void requestBird(View view){

        birdName = ((AutoCompleteTextView) this.findViewById(R.id.birdName)).getText().toString();


        if(birdName != null && birdName != "") {
            new RequestAsyncTask(view.getContext(), birdName, SpaceActivity.urlGet).execute();
        } else {
            Toast.makeText(
                    getBaseContext(),
                    "Request Not Made: No BirdName Provided", Toast.LENGTH_SHORT).show();
        }
    }

    public void displayMap(String reply){
        if(reply != null && !reply.equals("Error")) {
            Intent intent = new Intent(this, MapsActivity.class);
            intent.putExtra(EXTRA_MESSAGE, reply);
            intent.putExtra(BIRD_NAME,birdName);
            startActivity(intent);
        }
    }

    public String sendRequest(String message, String website){
        String serverResponse = "Error";
        HttpURLConnection urlConnection = null;
        BufferedReader inReader = null;
        try {
            //URL url = new URL("http://mz7xlyzuh2ua67.speedy.cloud/getbird?bird-name=blue%20jay");

            String editSpaceMessage = "";

            for (int i = 0; i < message.length(); i++){
                if (message.charAt(i) == ' '){
                    editSpaceMessage+= "%20";
                } else {
                    editSpaceMessage += message.charAt(i);
                }
            }

            URL url = new URL(website+editSpaceMessage.toLowerCase());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");

            serverResponse = "";
            String data;
            inReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
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
            alertDialog = new AlertDialog.Builder(this.context)
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
            //alertDialog.setMessage("Reply: " + requestReply);
            alertDialog.setMessage("Reply:");
            if(!alertDialog.isShowing())
            {
                alertDialog.show();
            }


            displayMap(requestReply);

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
