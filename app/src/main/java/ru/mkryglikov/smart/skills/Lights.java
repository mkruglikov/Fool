package ru.mkryglikov.smart.skills;

import android.content.Context;
import android.media.MediaPlayer;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import ru.mkryglikov.smart.Config;
import ru.mkryglikov.smart.R;

public class Lights {

    private final String RED = "BCM18";
    private final String GREEN = "BCM23";
    private final String BLUE = "BCM24";
    private final PeripheralManagerService manager = new PeripheralManagerService();
    private MediaPlayer soundOn, soundOff;

    public Lights(Context context) {
        this.soundOn = MediaPlayer.create(context, R.raw.on);
        this.soundOff = MediaPlayer.create(context, R.raw.off);
    }

    public void change(final String light, @Nullable final String onOff) {
        List<Gpio> diods = new LinkedList<>();
        Boolean state = null;
        try {
            switch (light) {
                case "red":
                    diods.add(manager.openGpio(RED));
                    break;
                case "green":
                    diods.add(manager.openGpio(GREEN));
                    break;
                case "blue":
                    diods.add(manager.openGpio(BLUE));
                    break;
                case "all":
                    diods.add(manager.openGpio(RED));
                    diods.add(manager.openGpio(GREEN));
                    diods.add(manager.openGpio(BLUE));
                    break;
            }
        } catch (IOException e) {
            Log.w("FUCK", "Не могу добавить Gpio в лист");
            e.printStackTrace();
        }
        if (onOff != null) {
            switch (onOff) {
                case "on":
                    state = true;
                    break;
                case "off":
                    state = false;
                    break;
            }
        }
        for (Gpio diod : diods) {
            try {
                if (state != null) {
                    diod.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                    diod.setActiveType(Gpio.ACTIVE_HIGH);
                    diod.setValue(state);
                } else {
                    diod.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                    diod.setActiveType(Gpio.ACTIVE_HIGH);
                    Log.d(Config.TAG, "DIOD VALUE: " + diod.getValue());
                    state = !diod.getValue();
                    diod.setValue(state);
                }
                if (state)
                    soundOn.start();
                else
                    soundOff.start();
                diod.close();
            } catch (IOException e) {
                Log.w("FUCK", "Не могу переключить диод");
                e.printStackTrace();
            }
        }
    }
}
