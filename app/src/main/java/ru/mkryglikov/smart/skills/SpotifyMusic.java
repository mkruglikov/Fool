package ru.mkryglikov.smart.skills;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

import ru.mkryglikov.smart.Config;


public class SpotifyMusic implements SpotifyPlayer.NotificationCallback, ConnectionStateCallback {
    private Player player;
    private Context context;
    private String token;
    private PlaybackEventListener listener;

    public interface PlaybackEventListener {
        void onPlaybackEvent(PlayerEvent playerEvent, Metadata metadata);
    }

    public SpotifyMusic(Context context, PlaybackEventListener listener) {
        this.context = context;
        this.listener = listener;
        String token = getToken();
        if (token != null) {
            init(token);
        }
    }

    private String getToken() {
        try {
            return new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try {
                        URL url = new URL("https://accounts.spotify.com/api/token");
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((Config.SPOTIFY_ID + ":" + Config.SPOTIFY_SECRET).getBytes("utf-8"), Base64.NO_WRAP));
                        OutputStream os = connection.getOutputStream();
                        os.write(("grant_type=refresh_token&refresh_token=" + Config.SPOTIFY_REFRESH_TOKEN).getBytes("UTF-8"));
                        try {
                            connection.connect();
                            int responseCode = connection.getResponseCode();
                            if (responseCode == 200) {

                                BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));

                                StringBuilder sbResponse = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    sbResponse.append(line).append('\n');
                                }
                                return new JSONObject(sbResponse.toString()).getString("access_token");
                            } else {
                                Log.w(Config.TAG, "Spotify getToken() код ответа != 200");
                                return null;
                            }
                        } finally {
                            connection.disconnect();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void init(final String accessToken) {
        com.spotify.sdk.android.player.Config playerConfig = new com.spotify.sdk.android.player.Config(context, accessToken, Config.SPOTIFY_ID);
        Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(SpotifyPlayer spotifyPlayer) {
                player = spotifyPlayer;
                player.addConnectionStateCallback(SpotifyMusic.this);
                player.addNotificationCallback(SpotifyMusic.this);
                token = accessToken;
            }

            @Override
            public void onError(Throwable throwable) {
                Log.w(Config.TAG, "Не могу инициализировать Spotify: " + throwable.getMessage());
            }
        });
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d(Config.TAG, "Playback event received: " + playerEvent.name());
        listener.onPlaybackEvent(playerEvent, player.getMetadata());
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d(Config.TAG, "Playback error received: " + error.name());
    }

    @Override
    public void onLoggedIn() {
        Log.d(Config.TAG, "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d(Config.TAG, "User logged out");
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.d(Config.TAG, "Login failed: " + error.name());
        init(getToken());
    }

    @Override
    public void onTemporaryError() {
        Log.d(Config.TAG, "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d(Config.TAG, "Received connection message: " + message);
    }

    public void destroy() {
        Spotify.destroyPlayer(this);
    }

    public void control(String musicControl, Boolean... onOff) {
        if (player != null) {
            switch (musicControl) {
                case "resume":
                    player.resume(null);
                    break;
                case "stop":
                case "pause":
                    player.pause(null);
                    break;
                case "next":
                    player.skipToNext(null);
                    break;
                case "previous":
                    player.skipToPrevious(null);
                    break;
                case "repeat":
                    try {
                        player.setRepeat(null, onOff[0]);
                    } catch (IndexOutOfBoundsException e) {
                        player.setRepeat(null, !player.getPlaybackState().isRepeating);
                    }
                    break;
                case "shuffle":
                    try {
                        player.setShuffle(null, onOff[0]);
                    } catch (IndexOutOfBoundsException e) {
                        player.setShuffle(null, !player.getPlaybackState().isShuffling);
                    }
                    break;
            }
        }
    }


    public void playRandom() {
        List<String> artists = getArtistsList();
        if (artists != null) {
            player.setShuffle(null, true);
            player.playUri(null, "spotify:artist:" + artists.get(new Random().nextInt(artists.size())), 0, 0);
            player.skipToNext(null);
        } else {
            Log.d(Config.TAG, "Нет артистов");
        }
    }

    private List<String> getArtistsList() {
        try {
            return new AsyncTask<Void, Void, List<String>>() {
                @Override
                protected List<String> doInBackground(Void... params) {
                    try {
                        URL url = new URL("https://api.spotify.com/v1/me/following?type=artist&limit=50");
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        connection.setRequestProperty("Authorization", "Bearer " + token);
                        try {
                            connection.connect();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
                            StringBuilder sbResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sbResponse.append(line).append('\n');
                            }

                            try {
                                JSONArray items = new JSONObject(sbResponse.toString()).getJSONObject("artists").getJSONArray("items");
                                List<String> artists = new ArrayList<>();
                                for (int i = 0; i < items.length(); i++) {
                                    JSONObject item = items.getJSONObject(i);
                                    artists.add(item.getString("id"));
                                }
                                return artists;
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


    public boolean playArtist(String artist) {
        String artistId = getArtistId(artist);
        if (artistId != null) {
            player.setShuffle(null, true);
            player.playUri(null, "spotify:artist:" + artistId, 0, 0);
            player.skipToNext(null);
            return true;
        } else {
            return false;
        }
    }

    private String getArtistId(final String artist) {
        try {
            return new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try {
                        URL url = new URL("https://api.spotify.com/v1/search?q=" + artist + "&type=artist");
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
                            StringBuilder sbResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sbResponse.append(line).append('\n');
                            }

                            try {
                                return new JSONObject(sbResponse.toString()).getJSONObject("artists").getJSONArray("items").getJSONObject(0).getString("id");
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


    public boolean playPlaylist(String playlist) {
        Map<String, String> ids = getPlaylistIds(playlist);
        if (ids != null) {
            player.setShuffle(null, true);
            player.playUri(null, "spotify:user:" + ids.get("userId") + ":playlist:" + ids.get("playlistId"), 0, 0);
            player.skipToNext(null);
            return true;
        } else {
            return false;
        }
    }

    private Map<String, String> getPlaylistIds(final String playlist) {
        try {
            return new AsyncTask<Void, Void, Map<String, String>>() {
                @Override
                protected Map<String, String> doInBackground(Void... params) {
                    try {
                        URL url = new URL("https://api.spotify.com/v1/search?q=" + playlist + "&type=playlist&limit=1");
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
                            StringBuilder sbResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sbResponse.append(line).append('\n');
                            }
                            try {
                                Map<String, String> ids = new HashMap<String, String>();
                                ids.put("playlistId", new JSONObject(sbResponse.toString()).getJSONObject("playlists").getJSONArray("items").getJSONObject(0).getString("id"));
                                ids.put("userId", new JSONObject(sbResponse.toString()).getJSONObject("playlists").getJSONArray("items").getJSONObject(0).getJSONObject("owner").getString("id"));
                                return ids;
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


    public boolean playTrack(String track) {
        String trackId = getTrackId(track);
        if (trackId != null) {
            player.playUri(null, "spotify:track:" + trackId, 0, 0);
            return true;
        } else {
            return false;
        }
    }

    private String getTrackId(final String track) {
        try {
            return new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try {
                        URL url = new URL("https://api.spotify.com/v1/search?q=" + track + "&type=track&limit=1");
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
                            StringBuilder sbResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sbResponse.append(line).append('\n');
                            }

                            try {
                                return new JSONObject(sbResponse.toString()).getJSONObject("tracks").getJSONArray("items").getJSONObject(0).getString("id");
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


    public boolean playAlbum(String album) {
        String albumId = getAlbumId(album);
        if (albumId != null) {
            player.setShuffle(null, false);
            player.playUri(null, "spotify:album:" + albumId, 0, 0);
            return true;
        } else {
            return false;
        }
    }

    private String getAlbumId(final String album) {
        try {
            return new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try {
                        URL url = new URL("https://api.spotify.com/v1/search?q=" + album + "&type=album&limit=1");
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(connection.getInputStream())));
                            StringBuilder sbResponse = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sbResponse.append(line).append('\n');
                            }

                            try {
                                return new JSONObject(sbResponse.toString()).getJSONObject("albums").getJSONArray("items").getJSONObject(0).getString("id");
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
