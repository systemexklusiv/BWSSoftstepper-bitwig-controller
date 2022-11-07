package de.davidrival.softstep.controller;

import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.api.SimpleConsolePrinter;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class Gestures extends SimpleConsolePrinter {

    public static final int FOOT_ON_MIN_PRESSURE = 1;

    public static final int LONG_PRESS_TIME = 500;

    @Getter
    @Setter
    private boolean isFootOnThanFootOff = false;

    @Getter
    @Setter
    private boolean isLongPress = false;

    private int footOnOffCounter = 0;

    public Gestures(ControllerHost hostOrNull) {
        super(hostOrNull);

        gestureTimer = new GestureTimer();
    }

    @Getter
    @Setter
    private GestureTimer gestureTimer;

    protected void check(int pressure) {
        this.isLongPress = checkLongPress(pressure);
        this.isFootOnThanFootOff = checkFootOnThanFootOff(pressure);
    }

    protected boolean checkLongPress(int pressure) {
        if (checkFootOn(pressure)) {
            if (!gestureTimer.isRunning()) {
                gestureTimer.start();
            }
            if (gestureTimer.getDeltaTime() >= LONG_PRESS_TIME) {
                gestureTimer.stop();
                return true;
            }
        }
        // if foot is lifted up before time reached cancel all
        else  {
                gestureTimer.stop();
        }
        return false;
    }

    protected boolean checkFootOnThanFootOff(int pressure) {
        if (checkFootOn(pressure)) {
            footOnOffCounter += 1;
            return false;
        } else {
            if (footOnOffCounter >= 1) {
                p(this.getClass().getSimpleName() + "  footOnAndOff detected!");
                footOnOffCounter = 0;

                return true;
            }
        }
        return false;
    }

    private boolean checkFootOn(int pressure) {
        return pressure >= FOOT_ON_MIN_PRESSURE;
    }

}
