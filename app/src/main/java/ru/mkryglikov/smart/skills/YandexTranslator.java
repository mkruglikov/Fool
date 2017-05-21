package ru.mkryglikov.smart.skills;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

import ru.mkryglikov.smart.Config;

public class YandexTranslator {
    public String translate(final String text, final String language) {
        try {
            return new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try {
                        URL url = new URL("https://translate.yandex.net/api/v1.5/tr.json/translate?key=" + Config.YANDEX_TRANSLATE_KEY + "&text=" + text + "&lang=" + language);
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
                            StringBuilder sbResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sbResponse.append(line).append('\n');
                            }

                            try {
                                JSONObject response = new JSONObject(sbResponse.toString());
                                JSONArray textArray = response.getJSONArray("text");
                                return textArray.getString(0);
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
