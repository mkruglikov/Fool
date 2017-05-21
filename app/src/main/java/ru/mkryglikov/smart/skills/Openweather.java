package ru.mkryglikov.smart.skills;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import ru.mkryglikov.smart.Config;

public class Openweather {
    public String getCurrentWeather() {
        try {
            return new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try {
                        URL url = new URL("http://api.openweathermap.org/data/2.5/weather?APPID=" + Config.OPEN_WEATHER_KEY + "&id=" + Config.OPEN_WEATHER_CITY + "&lang=ru&units=metric");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
                            StringBuilder sbResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sbResponse.append(line).append('\n');
                            }

                            try {
                                JSONObject response = new JSONObject(sbResponse.toString());
                                JSONArray weatherArray = response.getJSONArray("weather");
                                String description = weatherArray.getJSONObject(0).getString("description");
                                int tempMin = response.getJSONObject("main").getInt("temp_min");
                                int tempMax = response.getJSONObject("main").getInt("temp_max");
                                int windSpeed = response.getJSONObject("wind").getInt("speed");

                                StringBuilder sbResult = new StringBuilder();
                                sbResult.append(description);
                                sbResult.append(", ");
                                if (tempMin != tempMax) {
                                    sbResult.append("температура от ");
                                    sbResult.append(tempMin);
                                    sbResult.append(" до ");
                                    sbResult.append(tempMax);
                                    sbResult.append(" градусов");
                                } else {
                                    sbResult.append("температура ");
                                    sbResult.append(tempMin);
                                    sbResult.append(" градусов, ");
                                }
                                sbResult.append("ветер ");
                                sbResult.append(windSpeed);
                                sbResult.append(" метров в секунду.");

                                return sbResult.toString();
                            } catch (JSONException e) {
                                e.printStackTrace();
                                return null;
                            }

                        } finally {
                            connection.disconnect();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }.execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
