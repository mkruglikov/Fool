package ru.mkryglikov.smart.skills;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

import ru.mkryglikov.smart.Config;

public class Todoist {
    DateFormat utcDateFormat = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss");

    public List<JSONObject> getTasks(final String desiredDate, final String desiredTask) {
        try {
            return new AsyncTask<Void, Void, List<JSONObject>>() {
                @Override
                protected List<JSONObject> doInBackground(Void... params) {
                    try {
                        boolean isDateSet = false;
                        URL url = new URL("https://todoist.com/API/v7/sync?token=" + Config.TODOIST_KEY + "&resource_types=[\"items\"]");
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
                            StringBuilder sbResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sbResponse.append(line).append('\n');
                            }

                            JSONObject response = new JSONObject(URLDecoder.decode(sbResponse.toString(), "UTF-8"));
                            JSONArray itemsArray = response.getJSONArray("items");
                            List<JSONObject> itemsList = new LinkedList<>();
                            for (int i = 0; i < itemsArray.length(); i++) {
                                itemsList.add(itemsArray.getJSONObject(i));
                            }
                            Calendar desiredCalendar = Calendar.getInstance();
                            if (desiredDate != null && !desiredDate.equals("") && !desiredDate.equals(" ")) {
                                isDateSet = true;
                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                                try {
                                    desiredCalendar.setTime(df.parse(desiredDate));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (desiredTask != null && !desiredTask.equals("") && !desiredTask.equals(" ")) {
                                for (Iterator<JSONObject> iterator = itemsList.iterator(); iterator.hasNext(); ) {
                                    JSONObject task = iterator.next();
                                    try {
                                        Calendar itemDate = Calendar.getInstance();
                                        itemDate.setTime(utcDateFormat.parse(task.getString("due_date_utc").replace("  0000", "")));
                                        if (isDateSet) {
                                            if (itemDate.get(Calendar.DAY_OF_YEAR) != desiredCalendar.get(Calendar.DAY_OF_YEAR) || !task.getString("content").toLowerCase().replaceAll("[-+.^:,]", "").equals(desiredTask.toLowerCase())) {
                                                iterator.remove();
                                            }
                                        } else {
                                            if (!task.getString("content").toLowerCase().replaceAll("[-+.^:,]", "").equals(desiredTask.toLowerCase())) {
                                                iterator.remove();
                                            }
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        return null;
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                }
                            } else {
                                for (Iterator<JSONObject> iterator = itemsList.iterator(); iterator.hasNext(); ) {
                                    JSONObject task = iterator.next();
                                    try {
                                        Calendar itemDate = Calendar.getInstance();
                                        itemDate.setTime(utcDateFormat.parse(task.getString("due_date_utc").replace("  0000", "")));
                                        if (itemDate.get(Calendar.DAY_OF_YEAR) != desiredCalendar.get(Calendar.DAY_OF_YEAR)) {
                                            iterator.remove();
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        return null;
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                }
                            }
                            return itemsList;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        } catch (JSONException e) {
                            e.printStackTrace();
                            return null;
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

    public boolean addTask(final String task, final String date, final String time) {
        try {
            return new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        StringBuilder sb = new StringBuilder();
                        sb.append(task);
                        if (date != null && !date.equals(" ") && !date.equals(" ")) {
                            sb.append(" ");
                            sb.append(date);
                        }
                        if (time != null && !time.equals(" ") && !time.equals(" ")) {
                            sb.append(" ");
                            sb.append(time.substring(0, time.length() - 3)); // 00:00:00 to 00:00
                        }
                        URL url = new URL("https://todoist.com/API/v7/quick/add?token=" + Config.TODOIST_KEY + "&text=" + sb.toString());
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        try {
                            if (connection.getResponseCode() == 200) {
                                return true;
                            } else {
                                Log.w(Config.TAG, "Не смог добавить задачу: ответ не 200!");
                                return false;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        } finally {
                            connection.disconnect();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }.execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

}
