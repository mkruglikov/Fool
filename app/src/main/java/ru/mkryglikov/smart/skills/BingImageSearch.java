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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import ru.mkryglikov.smart.Config;

public class BingImageSearch {

    public List<String> getUrls(final String query) {
        try {
            return new AsyncTask<Void, Void, List<String>>() {
                @Override
                protected List<String> doInBackground(Void... params) {
                    try {
                        URL url = new URL("https://api.cognitive.microsoft.com/bing/v5.0/images/search?q=" + query);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestProperty("Ocp-Apim-Subscription-Key", Config.BING_KEY);
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
                            StringBuilder sbResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sbResponse.append(line).append('\n');
                            }

                            try {
                                List<String> urls = new LinkedList<>();
                                JSONObject response = new JSONObject(sbResponse.toString());
                                JSONArray valuesArray = response.getJSONArray("value");
                                for (int i = 0; i < valuesArray.length(); i++) {
                                    urls.add(valuesArray.getJSONObject(i).getString("thumbnailUrl"));
                                }
                                return urls;
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
