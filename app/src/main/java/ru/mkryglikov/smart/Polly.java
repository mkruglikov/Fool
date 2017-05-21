package ru.mkryglikov.smart;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;

import java.io.IOException;
import java.net.URL;

class Polly {
    private AudioManager audioManager;

    enum Lang {
        Ru("Maxim"),
        En("Joey"),
        Ja("Mizuki");

        private String language;

        Lang(String language) {
            this.language = language;
        }

        @Override
        public String toString() {
            return language;
        }
    }

    private static final Regions MY_REGION = Regions.US_WEST_2;
    private AmazonPollyPresigningClient client;

    Polly(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        client = new AmazonPollyPresigningClient(new CognitoCachingCredentialsProvider(
                context,
                Config.POLLY_COGNITO_POOL_ID,
                MY_REGION
        ));
    }

    void say(final String text, final Lang voiceId, final SpeechEndListener... listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest = new SynthesizeSpeechPresignRequest()
                        .withText(text)
                        .withVoiceId(voiceId.toString())
                        .withOutputFormat(OutputFormat.Mp3);

                URL presignedSynthesizeSpeechUrl = client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);
                MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);

                try {
                    mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
                } catch (IOException e) {
                    Log.e("FUCK", "Unable to set data source for the media player! " + e.getMessage());
                }
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / 2, 0);
                        mp.start();
                    }
                });

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, MainActivity.volume, 0);
                        if (listener.length > 0)
                            listener[0].onSpeechEnd();
                    }
                });
            }
        }).start();
    }

    interface SpeechEndListener {
        void onSpeechEnd();
    }
}
