package ru.mkryglikov.smart.skills;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

public class Wiki {
    public String getAnswer(final String query) {
        try {
            return new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try {
                        URL url = new URL("https://ru.wikipedia.org/w/api.php?action=opensearch&search=" + query + "&format=json&utf8=true");
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
                            StringBuilder sbResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sbResponse.append(line).append('\n');
                            }

                            try {
                                JSONArray responseArray = new JSONArray(sbResponse.toString());
                                JSONArray variants = responseArray.getJSONArray(2);
                                String[] resultSplitted = variants.getString(0).split(" — ");
                                return resultSplitted[resultSplitted.length - 1];
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
