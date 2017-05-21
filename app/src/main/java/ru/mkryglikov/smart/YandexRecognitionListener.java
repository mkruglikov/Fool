package ru.mkryglikov.smart;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;

import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.Recognition;
import ru.yandex.speechkit.Recognizer;
import ru.yandex.speechkit.RecognizerListener;

class YandexRecognitionListener implements RecognizerListener {
    private SpeechListener speechListener;
    private Recognizer recognizer;
    private MediaPlayer soundListenStart;
    private MediaPlayer soundListenStop;

    YandexRecognitionListener(Context context) {
        soundListenStart = MediaPlayer.create(context, R.raw.listen_start);
        soundListenStart.setAudioStreamType(AudioManager.STREAM_MUSIC);
        soundListenStop = MediaPlayer.create(context, R.raw.listen_stop);
        soundListenStop.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    interface SpeechListener {
        void onRecognitionDone(Error error, String speech);
    }

    void recognize(SpeechListener listener) {
        this.speechListener = listener;
        recognizer = Recognizer.create("ru-RU", "general", this);
        recognizer.start();
    }

    public void onRecordingBegin(Recognizer r) {
        soundListenStart.start();
    }

    public void onRecognitionDone(Recognizer r, Recognition results) {
        soundListenStop.start();
        speechListener.onRecognitionDone(null, results.getBestResultText());
        recognizer = null;
    }

    @Override
    public void onError(Recognizer recognizer, Error error) {
        soundListenStop.start();
        speechListener.onRecognitionDone(error, null);
        this.recognizer = null;
    }

    public void onRecordingDone(Recognizer r) {
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... params) {
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void aVoid) {
//                if (recognizer != null) {
//                    recognizer.cancel();
//                    speechListener.onRecognitionDone(null, "");
//                    recognizer = null;
//                }
//            }
//        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void onSoundDataRecorded(Recognizer r, byte[] data) {

    }

    public void onSpeechDetected(Recognizer r) {

    }

    public void onPowerUpdated(Recognizer r, float power) {

    }

    public void onPartialResults(Recognizer r, Recognition results, boolean endOfUtterance) {
//        if (results.getHypotheses() != null && results.getBestResultText().equals("отмена")) {
//            soundListenStop.start();
//            speechListener.onRecognitionDone(null, null);
//            this.recognizer = null;
//        }
    }
}

