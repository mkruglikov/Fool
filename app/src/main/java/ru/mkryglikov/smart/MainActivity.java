package ru.mkryglikov.smart;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.gson.JsonElement;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlayerEvent;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import ai.api.model.AIResponse;
import ai.api.model.Result;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;
import ru.mkryglikov.smart.skills.BingImageSearch;
import ru.mkryglikov.smart.skills.Lights;
import ru.mkryglikov.smart.skills.Openweather;
import ru.mkryglikov.smart.skills.Rfid;
import ru.mkryglikov.smart.skills.SpotifyMusic;
import ru.mkryglikov.smart.skills.Todoist;
import ru.mkryglikov.smart.skills.Wiki;
import ru.mkryglikov.smart.skills.YandexTranslator;
import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.SpeechKit;

public class MainActivity extends Activity implements edu.cmu.pocketsphinx.RecognitionListener, Rfid.RfidListener {

    private DateFormat utcDateFormat = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss");
    private ImageView iv1, iv2, iv3, ivListen, ivAlbumCover, ivPlayPause, ivPreviousTrack, ivNextTrack, ivChangeTrack, ivRepeatTrack, ivShuffleTracks, ivVolume;
    private TextView tvSpeech, tvTime, tvArtistTrack;
    private static AsyncTask<Void,Void,Void> atWake;
    private SpeechRecognizer wakeWordRecognizer;
    private boolean isRfidBusy = false;
    private static Handler timeHandler;
    private AudioManager audioManager;
    private SharedPreferences sPref;
    private Typeface notoTypeface;
    private TextSwitcher tsMain;
    private ProgressBar pbMain;
    public static int volume;
    private SeekBar sbVolume;
    private File assetsDir;
    private Assets assets;
//    private Button btn;

    private YandexTranslator yandexTranslator = new YandexTranslator();
    private BingImageSearch bingImageSearch = new BingImageSearch();
    private Openweather openweather = new Openweather();
    private Todoist todoist = new Todoist();
    private ApiAi apiAi = new ApiAi();
    private Wiki wiki = new Wiki();
    private Rfid rfid = new Rfid();
    private YandexRecognitionListener recognitionListener;
    private SpotifyMusic spotifyMusic;
    private Lights lights;
    private Polly polly;

    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final String WAKEUP = "wakeup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notoTypeface = Typeface.createFromAsset(getAssets(), "fonts/noto.otf");
        tvSpeech = (TextView) findViewById(R.id.tvSpeech);
        tvSpeech.setTypeface(notoTypeface);
        tvTime = (TextView) findViewById(R.id.tvTime);
        tvTime.setTypeface(notoTypeface);
        tvArtistTrack = (TextView) findViewById(R.id.tvArtistTrack);
        tvArtistTrack.setTypeface(notoTypeface);
        pbMain = (ProgressBar) findViewById(R.id.pbMain);
        tsMain = (TextSwitcher) findViewById(R.id.tsMain);
        tsMain.setFactory(new ViewSwitcher.ViewFactory() {
            public View makeView() {
                TextView tv = new TextView(MainActivity.this);
                tv.setGravity(Gravity.CENTER);
                tv.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER));
                tv.setTextSize(36);
                tv.setTextColor(Color.BLACK);
                tv.setTypeface(notoTypeface);
                return tv;
            }
        });

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            return;
        }

//        btn = (Button) findViewById(R.id.btn);
//        btn.setEnabled(false);

        initialize();
        spotifyMusic = new SpotifyMusic(this, new SpotifyMusic.PlaybackEventListener() {
            @Override
            public void onPlaybackEvent(PlayerEvent playerEvent, Metadata metadata) {
                switch (playerEvent) {
                    // Handle event type as necessary
                    case kSpPlaybackNotifyPlay:
                        ivPlayPause.setImageDrawable(getDrawable(R.drawable.ic_action_pause));
                        ivPlayPause.setTag(1);
                        break;
                    case kSpPlaybackNotifyPause:
                        ivPlayPause.setImageDrawable(getDrawable(R.drawable.ic_action_play));
                        ivPlayPause.setTag(0);
                        break;
                    case kSpPlaybackNotifyRepeatOn:
                        ivRepeatTrack.setTag(1);
                        ivRepeatTrack.setColorFilter(getColor(R.color.colorAccent));
                        break;
                    case kSpPlaybackNotifyRepeatOff:
                        ivRepeatTrack.setTag(0);
                        ivRepeatTrack.setColorFilter(Color.parseColor("#9e9e9e"));
                        break;
                    case kSpPlaybackNotifyShuffleOn:
                        ivShuffleTracks.setTag(1);
                        ivShuffleTracks.setColorFilter(getColor(R.color.colorAccent));
                        break;
                    case kSpPlaybackNotifyShuffleOff:
                        ivShuffleTracks.setTag(0);
                        ivShuffleTracks.setColorFilter(Color.parseColor("#9e9e9e"));
                        break;
                    case kSpPlaybackNotifyNext:
                    case kSpPlaybackNotifyPrev:
                    case kSpPlaybackNotifyMetadataChanged:
                        try {
                            tvArtistTrack.setText(metadata.currentTrack.artistName + " - " + metadata.currentTrack.name);
                            Picasso.with(MainActivity.this).load(metadata.currentTrack.albumCoverWebUrl).into(ivAlbumCover);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        sbVolume = (SeekBar) findViewById(R.id.sbVolume);
        sbVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        sbVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volume = progress;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        findViewById(R.id.btnExit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        polly = new Polly(this);
        lights = new Lights(this);
        recognitionListener = new YandexRecognitionListener(this);
//        lights.change("all", "off");

        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        am.setTimeZone(Config.TIMEZONE);
        Calendar now = Calendar.getInstance();
        String hours = now.get(Calendar.HOUR_OF_DAY) >= 10 ? String.valueOf(now.get(Calendar.HOUR_OF_DAY)) : "0" + String.valueOf(now.get(Calendar.HOUR_OF_DAY));
        String minutes = now.get(Calendar.MINUTE) >= 10 ? String.valueOf(now.get(Calendar.MINUTE)) : "0" + String.valueOf(now.get(Calendar.MINUTE));
        tvTime.setText(hours + ":" + minutes);

        timeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Calendar now = Calendar.getInstance();
                String hours = now.get(Calendar.HOUR_OF_DAY) >= 10 ? String.valueOf(now.get(Calendar.HOUR_OF_DAY)) : "0" + String.valueOf(now.get(Calendar.HOUR_OF_DAY));
                String minutes = now.get(Calendar.MINUTE) >= 10 ? String.valueOf(now.get(Calendar.MINUTE)) : "0" + String.valueOf(now.get(Calendar.MINUTE));
                tvTime.setText(hours + ":" + minutes);
            }
        };

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                timeHandler.sendMessage(new Message());
            }
        };
        timer.scheduleAtFixedRate(task, 0, 60 * 1000);

        iv1 = (ImageView) findViewById(R.id.iv1);
        iv2 = (ImageView) findViewById(R.id.iv2);
        iv3 = (ImageView) findViewById(R.id.iv3);

        ivAlbumCover = (ImageView) findViewById(R.id.ivAlbumCover);
        ivAlbumCover.setElevation(8);
        ivListen = (ImageView) findViewById(R.id.ivListen);
        ivVolume = (ImageView) findViewById(R.id.ivVolume);

        ivPreviousTrack = (ImageView) findViewById(R.id.ivPreviousTrack);
        ivPreviousTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spotifyMusic.control("previous");
            }
        });

        ivNextTrack = (ImageView) findViewById(R.id.ivNextTrack);
        ivNextTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spotifyMusic.control("next");
            }
        });

        ivPlayPause = (ImageView) findViewById(R.id.ivPlayPause);
        ivPlayPause.setTag(0);
        ivPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch ((int) v.getTag()) {
                    case 0:
                        spotifyMusic.control("resume");
                        break;
                    case 1:
                        spotifyMusic.control("pause");
                        break;
                }
            }
        });

        ivChangeTrack = (ImageView) findViewById(R.id.ivChangeTrack);
        ivChangeTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spotifyMusic.playRandom();
                if ((int) ivPlayPause.getTag() == 0)
                    spotifyMusic.control("pause");
            }
        });

        ivRepeatTrack = (ImageView) findViewById(R.id.ivRepeatTrack);
        ivRepeatTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spotifyMusic.control("repeat", ivRepeatTrack.getTag() == null || ((int) ivRepeatTrack.getTag()) == 0);
            }
        });

        ivShuffleTracks = (ImageView) findViewById(R.id.ivShuffleTracks);
        ivShuffleTracks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spotifyMusic.control("shuffle", ivShuffleTracks.getTag() == null || ((int) ivShuffleTracks.getTag()) == 0);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                    else
                        initialize();
                } else
                    finish();
                break;
            case PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
                    else
                        initialize();
                } else
                    finish();
                break;
        }
    }

    private void initialize() {
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected void onPreExecute() {
                pbMain.setVisibility(View.VISIBLE);
                tsMain.setText("Загрузка");
            }

            @Override
            protected Exception doInBackground(Void... params) {
                SpeechKit.getInstance().configure(MainActivity.this, Config.YANDEX_SPEECHKIT_KEY);
                try {
                    assets = new Assets(MainActivity.this);
                    assetsDir = assets.syncAssets();
                    wakeWordRecognizer = SpeechRecognizerSetup.defaultSetup()
                            .setAcousticModel(new File(assetsDir, "ru-ru"))
                            .setDictionary(new File(assetsDir, "ru.dic"))
                            .setRawLogDir(assetsDir)
                            .getRecognizer();
                    wakeWordRecognizer.addListener(MainActivity.this);
                    wakeWordRecognizer.addKeyphraseSearch(WAKEUP, Config.KEYPHRASE);
                } catch (IOException e) {
                    e.printStackTrace();
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception err) {
                if (err != null) {
                    tsMain.setText("Ошибка инициализации");
                    tvSpeech.setText(err.getMessage());
                } else {
                    rfid.setRfidListener(MainActivity.this, MainActivity.this);
                    tsMain.setText("");
                    pbMain.setVisibility(View.GONE);
//                    btn.setEnabled(true);
                    ivPreviousTrack.setVisibility(View.VISIBLE);
                    ivPlayPause.setVisibility(View.VISIBLE);
                    ivNextTrack.setVisibility(View.VISIBLE);
                    ivChangeTrack.setVisibility(View.VISIBLE);
                    ivRepeatTrack.setVisibility(View.VISIBLE);
                    ivShuffleTracks.setVisibility(View.VISIBLE);
                    ivVolume.setVisibility(View.VISIBLE);
                    sbVolume.setVisibility(View.VISIBLE);
                    findViewById(R.id.controlsDivider).setVisibility(View.VISIBLE);
                    spotifyMusic.playRandom();
                    spotifyMusic.control("next");
                    spotifyMusic.control("pause");
                    atWake = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected void onPreExecute() {
                            wakeWordRecognizer.stop();
                            wakeWordRecognizer.startListening(WAKEUP);
                        }

                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            tvSpeech.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out));
                            tvSpeech.setText("");
                        }
                    };
                    listenToWakeWord();
                }

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    //PocketSphinx wake word listener
    private void listenToWakeWord() {
        atWake.cancel(false);
        atWake.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    //Yandex speech listener
    private void listenToSpeech(YandexRecognitionListener.SpeechListener listener) {
        wakeWordRecognizer.stop();
        recognitionListener.recognize(listener);
        ivListen.setVisibility(View.VISIBLE);
        ivListen.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in));
        tvSpeech.setText("");
        tsMain.setText("");
        iv1.setVisibility(View.GONE);
        iv2.setVisibility(View.GONE);
        iv3.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        if (wakeWordRecognizer != null) {
            wakeWordRecognizer.cancel();
            wakeWordRecognizer.shutdown();
        }
        spotifyMusic.destroy();
        super.onDestroy();
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {
        if (!wakeWordRecognizer.getSearchName().equals(WAKEUP))
            listenToWakeWord();
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis != null && hypothesis.getHypstr().equals(Config.KEYPHRASE)) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / 2, 0);
            listenToSpeech(new YandexRecognitionListener.SpeechListener() {
                @Override
                public void onRecognitionDone(@Nullable Error error, String speech) {
                    ivListen.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out));
                    ivListen.setVisibility(View.GONE);
                    if (error != null) {
                        tsMain.setText("Ошибка Yandex recognizer");
                        tvSpeech.setText(error.getString());
                        return;
                    }
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                    listenToWakeWord();
                    if (speech != null && speech.length() > 0) {
                        doAction(apiAi.makeRequest(speech));
                        tvSpeech.setText(Character.toUpperCase(speech.charAt(0)) + speech.substring(1));
                    }
                }
            });
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {

    }

    @Override
    public void onError(Exception e) {
        tsMain.setText("Ошибка PocketSphinx");
        tvSpeech.setText(e.getMessage());
        listenToWakeWord();
    }

    @Override
    public void onTimeout() {
        listenToWakeWord();
    }

    private static DateFormatSymbols ruMothsFormatSymbols = new DateFormatSymbols() {
        @Override
        public String[] getMonths() {
            return new String[]{"января", "февраля", "марта", "апреля", "мая", "июня",
                    "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        }
    };

    // Do some actions based on api.ai response
    void doAction(AIResponse response) {
        if (response != null) {
            final Result result = response.getResult();
            final String speech = result.getFulfillment().getSpeech();
            final HashMap<String, JsonElement> params = result.getParameters();

            if (params != null && !params.isEmpty()) {
                for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                    Log.d(Config.TAG, entry.getKey() + " : " + entry.getValue().toString());
                }
            }
            //todo
            if (speech != null && !speech.equals("") && ((params.get("simplified") != null && !params.get("simplified").getAsString().equals("one-color")) || (params.get("simplified") == null))) {
                Log.d(Config.TAG, "doAction: SPEECH");
                tsMain.setText(speech);
                polly.say(speech, Polly.Lang.Ru, new Polly.SpeechEndListener() {
                    @Override
                    public void onSpeechEnd() {
                        tsMain.setText("");
                    }
                });
                return;
            }

            if (result.getMetadata() != null && result.getMetadata().getIntentName() != null) {
                final String intent = result.getMetadata().getIntentName();
                Log.d(Config.TAG, "intent: " + intent);
                switch (intent) {
                    case "light":
                        try {
                            lights.change(params.get("light").getAsString(), params.get("onoff") == null ? null : params.get("onoff").getAsString());
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        break;

                    case "wiki":
                        try {
                            String answer = (wiki.getAnswer(params.get("query").getAsString()));
                            if (answer != null && !answer.equals("")) {
                                polly.say(answer, Polly.Lang.Ru);
                                tsMain.setText(Character.toUpperCase(answer.charAt(0)) + answer.substring(1));
                            } else {
                                answer = wiki.getAnswer(yandexTranslator.translate(params.get("query").getAsString(), "en"));
                                if (answer != null && !answer.equals("")) {
                                    polly.say(answer, Polly.Lang.Ru);
                                    tsMain.setText(Character.toUpperCase(answer.charAt(0)) + answer.substring(1));
                                } else {
                                    polly.say("Я не знаю", Polly.Lang.Ru);
                                    tsMain.setText("Я не знаю");
                                }
                            }
                        } catch (NullPointerException e) {
                            tsMain.setText("Ошибка wiki");
                            e.printStackTrace();
                        }
                        break;

                    case "translate":
                        String translated = yandexTranslator.translate(params.get("query").getAsString(), params.get("language").getAsString());
                        switch (params.get("language").getAsString()) {
                            case "ru":
                                polly.say(translated, Polly.Lang.Ru);
                                break;
                            case "en":
                                polly.say(translated, Polly.Lang.En);
                                break;
                            case "ja":
                                polly.say(translated, Polly.Lang.Ja);
                                break;
                        }
                        tsMain.setText(translated);
                        break;

                    case "time":
                        polly.say(tvTime.getText().toString(), Polly.Lang.Ru);
                        break;

                    case "weather":
                        String weatherResult = openweather.getCurrentWeather();
                        polly.say(weatherResult, Polly.Lang.Ru);
                        tsMain.setText(weatherResult);
                        break;

                    case "imageSearch": //FixMe костыль
                        if (params.get("query").getAsString() == null)
                            return;
                        iv1.setVisibility(View.VISIBLE);
                        iv2.setVisibility(View.VISIBLE);
                        iv3.setVisibility(View.VISIBLE);
                        List<String> urls = bingImageSearch.getUrls(params.get("query").getAsString());
                        if (urls.size() > 3) {
                            int first = new Random().nextInt(urls.size());
                            int second, third;
                            do {
                                second = new Random().nextInt(urls.size());
                            } while (second == first);
                            do {
                                third = new Random().nextInt(urls.size());
                            } while (third == first || third == second);
                            Picasso.with(MainActivity.this).load(urls.get(first)).into(iv1);
                            Picasso.with(MainActivity.this).load(urls.get(new Random().nextInt(urls.size()))).into(iv2);
                            Picasso.with(MainActivity.this).load(urls.get(new Random().nextInt(urls.size()))).into(iv3);
                        } else {
                            String err = "Я не знаю как это выглядит";
                            polly.say(err, null);
                            tsMain.setText(err);
                        }
                        break;
                    case "tasks.get":
                        final String desiredDate = params.get("date") == null ? null : params.get("date").getAsString();
                        final String desiredTask = params.get("task") == null ? null : params.get("task").getAsString();


                        new AsyncTask<Void, Void, String>() {
                            @Override
                            protected void onPreExecute() {
                                tvSpeech.setVisibility(View.GONE);
                                pbMain.setVisibility(View.VISIBLE);
                            }

                            @Override
                            protected String doInBackground(Void... params) {
                                StringBuilder sb = new StringBuilder();
                                List<JSONObject> tasks = todoist.getTasks(desiredDate, desiredTask);
                                if (tasks == null)
                                    return null;
//                                String[] numerals = {"Во-первых", "Во-вторых", "В-третьих", "В-четвертых", "В-пятых", "В-шестых", "В-седьмых", "В-восьмых", "В-девятых", "В-десятых"};
                                String[] numerals = {"Первое", "Второе", "Третье", "Четвёртое", "Пятое", "Шестое", "Седьмое", "Восьмое", "Девятое", "Десятое"};
                                try {
                                    if (desiredTask != null && !desiredTask.equals("") && !desiredTask.equals(" ")) {
                                        if (tasks.size() == 1) {
                                            JSONObject task = tasks.get(0);
                                            Calendar itemDate = Calendar.getInstance();
                                            itemDate.setTime(utcDateFormat.parse(task.getString("due_date_utc").replace("  0000", "")));

                                            //noinspection WrongConstant
                                            if (itemDate.get(Calendar.HOUR_OF_DAY) == 20 && itemDate.get(Calendar.MINUTE) == 59) {
                                                sb.append("Конкретное время не назначено");
                                            } else {
                                                sb.append("В ");
                                                sb.append(itemDate.get(Calendar.HOUR_OF_DAY) + 3 >= 10 ? itemDate.get(Calendar.HOUR_OF_DAY) + 3 : "0" + itemDate.get(Calendar.HOUR_OF_DAY) + 3);
                                                sb.append(":");
                                                sb.append(itemDate.get(Calendar.MINUTE) >= 10 ? itemDate.get(Calendar.MINUTE) : "0" + itemDate.get(Calendar.MINUTE));
                                            }
                                        } else if (tasks.size() > 1) {

                                            Collections.sort(tasks, new Comparator<JSONObject>() {
                                                Calendar c1 = Calendar.getInstance();
                                                Calendar c2 = Calendar.getInstance();

                                                @Override
                                                public int compare(JSONObject o1, JSONObject o2) {
                                                    try {
                                                        c1.setTime(utcDateFormat.parse(o1.getString("due_date_utc").replace("  0000", "")));
                                                        c2.setTime(utcDateFormat.parse(o2.getString("due_date_utc").replace("  0000", "")));

                                                        if (c1.get(Calendar.DAY_OF_MONTH) > c2.get(Calendar.DAY_OF_MONTH))
                                                            return 1;
                                                        else if (c1.get(Calendar.DAY_OF_MONTH) < c2.get(Calendar.DAY_OF_MONTH))
                                                            return -1;
                                                        else
                                                            return 0;
                                                    } catch (ParseException e) {
                                                        e.printStackTrace();
                                                        return 0;
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                        return 0;
                                                    }
                                                }
                                            });

                                            int tasksWithoutTime = 0;
                                            String[] notime = {"но конкретное время не назначено", "но время опять не задано", "снова нет времени"};
                                            for (JSONObject task : tasks) {
                                                Calendar itemDate = Calendar.getInstance();
                                                Calendar now = Calendar.getInstance();
                                                itemDate.setTime(utcDateFormat.parse(task.getString("due_date_utc").replace("  0000", "")));

                                                //noinspection WrongConstant
                                                if (itemDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                                                    sb.append("Сегодня");
                                                } else if (itemDate.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR) == 1) {
                                                    sb.append("Завтра");
                                                } else if (itemDate.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR) == 2) {
                                                    sb.append("Послезавтра");
                                                } else {
                                                    sb.append(itemDate.get(Calendar.DAY_OF_MONTH));
                                                    sb.append(" ");
                                                    sb.append(ruMothsFormatSymbols.getMonths()[itemDate.get(Calendar.MONTH)]);
                                                }

                                                //noinspection WrongConstant
                                                if (itemDate.get(Calendar.HOUR_OF_DAY) == 20 && itemDate.get(Calendar.MINUTE) == 59) {
                                                    sb.append(", ");
                                                    try {
                                                        sb.append(notime[tasksWithoutTime++]);
                                                    } catch (IndexOutOfBoundsException e) {
                                                        sb.append("время не задано");
                                                        e.printStackTrace();
                                                    } finally {
                                                        sb.append(". ");
                                                    }
                                                } else {
                                                    sb.append(" в ");
                                                    sb.append(itemDate.get(Calendar.HOUR_OF_DAY) + 3 >= 10 ? itemDate.get(Calendar.HOUR_OF_DAY) + 3 : "0" + itemDate.get(Calendar.HOUR_OF_DAY) + 3);
                                                    sb.append(":");
                                                    sb.append(itemDate.get(Calendar.MINUTE) >= 10 ? itemDate.get(Calendar.MINUTE) : "0" + itemDate.get(Calendar.MINUTE));
                                                    sb.append(". ");
                                                }
                                            }
                                        }
                                    } else {
                                        if (tasks.size() == 1) {
                                            JSONObject task = tasks.get(0);
                                            Calendar itemDate = Calendar.getInstance();
                                            itemDate.setTime(utcDateFormat.parse(task.getString("due_date_utc").replace("  0000", "")));
                                            sb.append(task.getString("content"));

                                            //noinspection WrongConstant
                                            if (itemDate.get(Calendar.HOUR_OF_DAY) != 20 && itemDate.get(Calendar.MINUTE) != 59) {
                                                sb.append(" в ");
                                                sb.append(itemDate.get(Calendar.HOUR_OF_DAY) + 3 >= 10 ? itemDate.get(Calendar.HOUR_OF_DAY) + 3 : "0" + itemDate.get(Calendar.HOUR_OF_DAY) + 3);
                                                sb.append(":");
                                                sb.append(itemDate.get(Calendar.MINUTE) >= 10 ? itemDate.get(Calendar.MINUTE) : "0" + itemDate.get(Calendar.MINUTE));
                                            }
                                        } else {
                                            int i = 0;
                                            for (JSONObject task : tasks) {
                                                try {
                                                    sb.append(numerals[i++]);
                                                } catch (NullPointerException e) {
                                                    sb.append("Затем");
                                                    e.printStackTrace();
                                                }
                                                sb.append(" - ");
                                                String splittedTask[] = task.getString("content").split(".");
                                                if (splittedTask.length == 0) {
                                                    sb.append(task.getString("content"));
                                                } else if (splittedTask[0].length() > 3) {
                                                    sb.append(splittedTask[0]);
                                                } else {
                                                    sb.append(splittedTask[0]);
                                                    sb.append(".");
                                                    sb.append(splittedTask[1]);
                                                }
                                                sb.append(". ");
                                            }
                                        }
                                    }
                                } catch (JSONException | ParseException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                                Log.d(Config.TAG, sb.toString());
                                return sb.toString();
                            }

                            @Override
                            protected void onPostExecute(String tasks) {
                                pbMain.setVisibility(View.GONE);
                                tvSpeech.setVisibility(View.VISIBLE);
                                if (tasks != null && !tasks.equals("") && !tasks.equals(" ")) {
                                    polly.say(tasks, Polly.Lang.Ru);
                                    tsMain.setText(tasks);
                                } else {
                                    tsMain.setText("Нет задач");
                                }
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        break;
                    case "tasks.add":
                        final String task = params.get("task") == null ? null : Character.toUpperCase(params.get("task").getAsString().charAt(0)) + params.get("task").getAsString().substring(1);
                        final String date = params.get("date") == null ? null : params.get("date").getAsString();
                        final String time = params.get("time") == null ? null : params.get("time").getAsString();
                        if (todoist.addTask(task, date, time)) {
                            String success = "Задача добавлена";
                            polly.say(success, Polly.Lang.Ru);
                            tsMain.setText(success);
                        } else {
                            String error = "Не получилось добавить задачу";
                            polly.say(error, Polly.Lang.Ru);
                            tsMain.setText(error);
                        }
                        break;
                    case "music.playlist":
                        if (params.get("query") != null) {
                            boolean isFound = spotifyMusic.playPlaylist(params.get("query").getAsString());
                            if (!isFound) {
                                isFound = spotifyMusic.playPlaylist(yandexTranslator.translate(params.get("query").getAsString(), "en"));
                                if (!isFound) {
                                    polly.say("Я не нашёл такого плейлиста", Polly.Lang.Ru);
                                    tsMain.setText("Я не нашёл такого плейлиста");
                                }
                            }
                        }
                        break;
                    case "music.track":
                        if (params.get("query") != null) {
                            boolean isFound = spotifyMusic.playTrack(params.get("query").getAsString());
                            if (!isFound) {
                                isFound = spotifyMusic.playTrack(yandexTranslator.translate(params.get("query").getAsString(), "en"));
                                if (!isFound) {
                                    polly.say("Я не нашёл такой песни", Polly.Lang.Ru);
                                    tsMain.setText("Я не нашёл такой песни");
                                }
                            }
                        }
                        break;
                    case "music.album":
                        if (params.get("query") != null) {
                            boolean isFound = spotifyMusic.playAlbum(params.get("query").getAsString());
                            if (!isFound) {
                                isFound = spotifyMusic.playAlbum(yandexTranslator.translate(params.get("query").getAsString(), "en"));
                                if (!isFound) {
                                    polly.say("Я не нашёл такого альбома", Polly.Lang.Ru);
                                    tsMain.setText("Я не нашёл такого альбома");
                                }
                            }
                        }
                        break;
                    case "music.certain":
                        if (params.get("query") != null) {  //FIXME
                            if (!spotifyMusic.playArtist(params.get("query").getAsString())) {
                                if (!spotifyMusic.playArtist(yandexTranslator.translate(params.get("query").getAsString(), "en"))) {
                                    if (!spotifyMusic.playTrack(params.get("query").getAsString())) {
                                        if (!spotifyMusic.playTrack(yandexTranslator.translate(params.get("query").getAsString(), "en"))) {
                                            if (!spotifyMusic.playAlbum(params.get("query").getAsString())) {
                                                if (!spotifyMusic.playAlbum(yandexTranslator.translate(params.get("query").getAsString(), "en"))) {
                                                    if (!spotifyMusic.playPlaylist(params.get("query").getAsString())) {
                                                        if (!spotifyMusic.playPlaylist(yandexTranslator.translate(params.get("query").getAsString(), "en"))) {
                                                            polly.say("Я не нашёл такой музыки", Polly.Lang.Ru);
                                                            tsMain.setText("Я не нашёл такой музыки");
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    case "music.control":
                        if (params.get("musicControl") != null && !params.get("musicControl").getAsString().equals("volume_up") && !params.get("musicControl").getAsString().equals("volume_down")) {
                            spotifyMusic.control(params.get("musicControl").getAsString());
                        } else if (params.get("musicControl") != null) {
                            switch (params.get("musicControl").getAsString()) {
                                case "volume_up":
                                    for (int i = 1; i <= Config.VOLUME_CHANGE_STEP; i++) {
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume + i, 0);
                                        try {
                                            Thread.sleep(200);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    sbVolume.setProgress(volume + Config.VOLUME_CHANGE_STEP, true);
                                    break;
                                case "volume_down":
                                    for (int i = 1; i <= Config.VOLUME_CHANGE_STEP; i++) {
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume - i, 0);
                                        try {
                                            Thread.sleep(200);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    sbVolume.setProgress(volume - Config.VOLUME_CHANGE_STEP, true);
                                    break;
                            }
                        }
                        break;
                    case "music.any":
                        spotifyMusic.playRandom();
                        break;
                    default:
                        tsMain.setText("Необработанная команда");
                        break;
                }
            }
        }
    }

    @Override
    public void onRfidDetected(int[] uuids) {
        if (isRfidBusy) return;
        sPref = getPreferences(MODE_PRIVATE);
        final String uuid = Arrays.toString(uuids);
        tvSpeech.setText(uuid);
        String cardName = sPref.getString(uuid, null);

        if (cardName == null || cardName.equals("")) {
            isRfidBusy = true;
            polly.say("Новая ме тка, сохранить её?", Polly.Lang.Ru, new Polly.SpeechEndListener() { //Пробел в слове "метка" для правильного ударения
                @Override
                public void onSpeechEnd() {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / 2, 0);
                    listenToSpeech(new YandexRecognitionListener.SpeechListener() {
                        @Override
                        public void onRecognitionDone(@Nullable Error error, String answer) {
                            ivListen.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out));
                            ivListen.setVisibility(View.GONE);
                            if (error != null) {
                                isRfidBusy = false;
                                tsMain.setText("Ошибка Yandex recognizer");
                                tvSpeech.setText(error.getString());
                                return;
                            }
                            switch (answer) {
                                case "конечно":
                                case "добавь":
                                case "ага":
                                case "да":
                                    polly.say("Как назвать?", Polly.Lang.Ru, new Polly.SpeechEndListener() {
                                        @Override
                                        public void onSpeechEnd() {
                                            listenToSpeech(new YandexRecognitionListener.SpeechListener() {
                                                @Override
                                                public void onRecognitionDone(@Nullable Error error, String name) {
                                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                                                    ivListen.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out));
                                                    ivListen.setVisibility(View.GONE);
                                                    Editor ed = sPref.edit();
                                                    ed.putString(uuid, name);
                                                    if (ed.commit()) {
                                                        tsMain.setText("Сохранил");
                                                        tvSpeech.setText(name);
                                                    } else {
                                                        tsMain.setText("Ошибка при сохранении");
                                                    }
                                                    listenToWakeWord();
                                                    isRfidBusy = false;
                                                }
                                            });
                                        }
                                    });
                                    tsMain.setText("Как назвать?");
                                    break;
                                default:
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                                    listenToWakeWord();
                                    isRfidBusy = false;
                                    break;
                            }
                        }
                    });
                }
            });
            tsMain.setText("Новая метка, сохранить её?");
        } else {
            tsMain.setText(Character.toUpperCase(cardName.charAt(0)) + cardName.substring(1));
            tvSpeech.setText(uuid);
        }
    }
}
