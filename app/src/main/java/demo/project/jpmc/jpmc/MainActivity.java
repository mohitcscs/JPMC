package demo.project.jpmc.jpmc;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private String MY_PREFS_NAME  = "City_Name";
    private String PREFS_CITY_KEY  = "CityName";

    private EditText cityNameEditText;
    private Button getWeatherDataButton;
    private TextView weatherResults;

    private String cityName;

    private String mainWeather;
    private String description;
    private String tempMin;
    private String tempMax;

    //BETTER Approaches available
    private ProgressDialog pDialog;

    // URL to get contacts JSON
    private static String url = "http://api.openweathermap.org/data/2.5/forecast?q=%s&appid=562c62c414e0b091d365a668a164958d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cityNameEditText = (EditText) findViewById(R.id.cityNameEditText);
        getWeatherDataButton = (Button) findViewById(R.id.getWeatherDataButton);
        weatherResults = (TextView) findViewById(R.id.weatherResults);

        getValueFromPreference(); //Read value of last saved city

        //If city name is not empty, this means last searched city is available. And load that city.
        if (!TextUtils.isEmpty(cityName))
        {
            cityNameEditText.setText(cityName);
            makeWeatherApiCall();
        }

        //Click listener for button
        getWeatherDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeWeatherApiCall();
            }
        });
    }

    /**
     * Make api call to get the weather details
     */
    private void makeWeatherApiCall()
    {
        cityName = cityNameEditText.getText().toString();
        if (TextUtils.isEmpty(cityName))
        {
            Toast.makeText(getApplicationContext(),
                    "Please enter some city name",
                    Toast.LENGTH_LONG)
                    .show();
        }
        else
        {
            // Parsing the string url with the cityname and apppending the country as per api docs
            url = String.format(url, cityName + ",US");
            new GetWeatherData().execute();
        }
    }

    /**
     * Async task class to get json by making HTTP call
     * Creating inner class to avoid the interface callback mechanism
     */
    private class GetWeatherData extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    // Getting JSON Array node
                    if(jsonObj.has("list"))
                    {
                        JSONArray records = jsonObj.getJSONArray("list");
                        JSONObject firstRecord = records.getJSONObject(0);
                        if (firstRecord.has("weather"))
                        {
                            JSONArray jsonArray = firstRecord.getJSONArray("weather");
                            JSONObject weatherObject = jsonArray.getJSONObject(0);
                            description = weatherObject.getString("description");
                            mainWeather = weatherObject.getString("main");
                        }

                        if (firstRecord.has("main"))
                        {
                            JSONObject mainObject = firstRecord.getJSONObject("main");
                            tempMin = mainObject.getString("temp_min");
                            tempMax = mainObject.getString("temp_max");
                        }
                    }

                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();

            if (weatherResults != null)
            {
                weatherResults.setText("Main Weather Condition:: " + mainWeather + "\n" +
                                        "Description:: " + description + "\n" +
                                        "Temp Min:: " + tempMin + "\n" +
                                        "Temp Max:: " + tempMax);
            }
            writeValueToPreference();
        }

    }

    /**
     * Write the last city name into preferences
     */
    private void writeValueToPreference()
    {
        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(PREFS_CITY_KEY, cityName);
        editor.apply();
        editor.commit();
    }

    /**
     * Read the value from shared preference
     */
    private void getValueFromPreference()
    {
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        cityName = prefs.getString(PREFS_CITY_KEY, "");
    }
}
