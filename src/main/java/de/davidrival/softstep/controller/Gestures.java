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
        pressure, footOn, doubleTrigger, longPress, incDec
    }

    private int pressure = 0;

    private boolean isFootOn = false;

    private boolean isDoubleTrigger = false;

    private boolean isLongPress = false;

    private int isIncDec = 0;


    public Gestures(ControllerHost hostOrNull) {
        super(hostOrNull);
    }

    public void reset(Softstep1Pad pad) {
        pressure = 0;
        isFootOn = false;
        isLongPress = false;
        isDoubleTrigger = false;
        isIncDec = 0;
        setFootOnDir(pad, -1);
        setlongPressDir(pad, -1);
        setDoubleTriggerDir(pad, -1);
        p("reset pad: " + pad);
    }

    public boolean set(Softstep1Pad pad) {
        Map<Integer, Integer> dirs = pad.getDirections();

        this.pressure = dirs.get(pad.getMinData1() + GestureOffsets.pressure.ordinal());

//        if(isFootOn && (pressure <= OFF_THRESHOLD)  ) {
        if (isFootOn) {
            setFootOnDir(pad, 0);
            this.isFootOn = false;
            p("! reset FOOT_ON on pad: " + pad);
        }
        if (isLongPress) {
            setlongPressDir(pad, 0);
            this.isLongPress = false;
            p("! reset LONG_PRESS on pad: " + pad);
        }
        if (isDoubleTrigger) {
            setDoubleTriggerDir(pad, 0);
            this.isDoubleTrigger = false;
            p("! reset DOUBLE_TRIGGER on pad: " + pad);
        }

        if (dirs.get(pad.getMinData1() + GestureOffsets.longPress.ordinal()) > OFF_THRESHOLD) {
            this.isLongPress = true;
            p("! Found LONG_PRESS on pad: " + pad);
        } else {
            if (dirs.get(pad.getMinData1() + GestureOffsets.doubleTrigger.ordinal()) > OFF_THRESHOLD) {
                this.isDoubleTrigger = true;
                p("! DOUBLE_TRIGGER on pad: " + pad);
            } else if (dirs.get(pad.getMinData1() + GestureOffsets.footOn.ordinal()) > OFF_THRESHOLD) {
                this.isFootOn = true;
                p("! FOOT_ON on pad: " + pad);
            }
        }


        this.isIncDec = dirs.get(pad.getMinData1() + GestureOffsets.incDec.ordinal());

        return true;
    }

    private Integer setFootOnDir(Softstep1Pad pad, int value) {
        return pad.getDirections().put(pad.getMinData1() + GestureOffsets.footOn.ordinal(), value);
    }

    private Integer setlongPressDir(Softstep1Pad pad, int value) {
        return pad.getDirections().put(pad.getMinData1() + GestureOffsets.longPress.ordinal(), value);
    }

    private Integer setDoubleTriggerDir(Softstep1Pad pad, int value) {
        return pad.getDirections().put(pad.getMinData1() + GestureOffsets.doubleTrigger.ordinal(), value);
    }


}
