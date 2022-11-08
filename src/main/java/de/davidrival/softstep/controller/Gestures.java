package de.davidrival.softstep.controller;

import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.api.SimpleConsolePrinter;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
public class Gestures extends SimpleConsolePrinter {

    private static final int AMOUNT_GESTURES_PER_PAD = 5;
    public static final int OFF_THRESHOLD = 10;

    enum GestureOffsets {
        pressure, footOn, longPress, doubleTrigger, incDec
    }

    private int pressure  = 0;

    private boolean isFootOn = false;

    private boolean isLongPress = false;

    private boolean isDoubleTrigger = false;

    private int  isIncDec = 0;


    public Gestures(ControllerHost hostOrNull) {
        super(hostOrNull);
    }

    public boolean set(Softstep1Pad pad) {
        Map<Integer,Integer> dirs = pad.getDirections();

        this.pressure = dirs.get(GestureOffsets.pressure.ordinal());

        if(isFootOn && this.pressure < OFF_THRESHOLD) {
            dirs.put(GestureOffsets.footOn.ordinal(), 0);
            isFootOn = false;
        }
        if(isLongPress && this.pressure < OFF_THRESHOLD) {
            dirs.put(GestureOffsets.longPress.ordinal(), 0);
            isLongPress = false;
        }
        if(isDoubleTrigger && this.pressure < OFF_THRESHOLD) {
            dirs.put(GestureOffsets.doubleTrigger.ordinal(), 0);
            isDoubleTrigger = false;
        }

        this.isFootOn = dirs.get(GestureOffsets.footOn.ordinal()) > OFF_THRESHOLD;
        this.isLongPress = dirs.get(GestureOffsets.longPress.ordinal()) > OFF_THRESHOLD;
        this.isDoubleTrigger = dirs.get(GestureOffsets.doubleTrigger.ordinal()) > OFF_THRESHOLD;
        this.isIncDec = dirs.get(GestureOffsets.incDec.ordinal());

        return true;
    }



}
