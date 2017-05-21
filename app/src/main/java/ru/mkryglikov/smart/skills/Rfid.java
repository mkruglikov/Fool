package ru.mkryglikov.smart.skills;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.galarzaa.androidthings.Rc522;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

import ru.mkryglikov.smart.Config;

public class Rfid {
    private Rc522 rc522;
    private RfidListener listener;

    public interface RfidListener {
        void onRfidDetected(int[] uuid);
    }

    public Rfid() {
        this.listener = null;
    }

    public void setRfidListener(RfidListener listener, Context context) {
        this.listener = listener;
        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            SpiDevice spiDevice = pioService.openSpiDevice("SPI0.0");
            Gpio resetPin = pioService.openGpio("BCM25");
            rc522 = new Rc522(context, spiDevice, resetPin);
            read();
        } catch (IOException e) {
            Log.w(Config.TAG, "SPI0.0 in use");
            e.printStackTrace();
        }
    }

    private void read() {
        new AsyncTask<Void, int[], Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                while (true) {
                    boolean success = rc522.request();
                    if (success) {
                        success = rc522.antiCollisionDetect();
                        if (success) {
                            byte[] uuid = rc522.getUuid();
                            int[] bytes = new int[16];
                            for (int i = 0; i < bytes.length; i++) {
                                bytes[i] = (int) uuid[i];
                            }
                            publishProgress(bytes);
                        }
                    }
                    try {
                        Thread.sleep(Config.RFID_SCAN_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            protected void onProgressUpdate(int[]... values) {
                if (listener != null) {
                    listener.onRfidDetected(values[0]);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
