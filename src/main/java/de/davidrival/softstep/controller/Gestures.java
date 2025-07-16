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

    private static final int AMOUNT_GESTURES_PER_PAD = 4;
    public static final int EXPECTED_ON_VALUE = 127;

    public enum GestureOffsets {
        pressure, footOn, doubleTrigger, longPress
    }

    private int pressure = 0;

    private boolean isFootOn = false;

    private boolean isDoubleTrigger = false;

    private boolean isLongPress = false;

    private boolean isAnyPress = false;



    public Gestures(ControllerHost hostOrNull) {
        super(hostOrNull);
    }

    public void reset(Softstep1Pad pad) {
        pressure = 0;
        isFootOn = false;
        footOnCounter = 0;
        isLongPress = false;
        isDoubleTrigger = false;
        isAnyPress = false;
        setFootOnDir(pad, -1);
        setlongPressDir(pad, -1);
        setDoubleTriggerDir(pad, -1);
//        p("reset pad: " + pad);
    }

    public boolean set(Softstep1Pad pad) {
        Map<Integer, Integer> dirs = pad.getDirections();

        this.pressure = dirs.get(pad.getMinData1() + GestureOffsets.pressure.ordinal());

        // Simple detection: any pressure above threshold = press
        int maxPressure = pad.calcMaxPressureOfDirections(dirs);
        this.isAnyPress = maxPressure > 10; // Threshold of 10 for any press

//        if(isFootOn && (pressure <= OFF_THRESHOLD)  ) {
        if (isFootOn) {
            setFootOnDir(pad, 0);
            this.isFootOn = false;
//            p("! reset FOOT_ON on pad: " + pad);
        }
        if (isLongPress) {
            setlongPressDir(pad, 0);
            this.isLongPress = false;
//            p("! reset LONG_PRESS on pad: " + pad);
        }
        if (isDoubleTrigger) {
            setDoubleTriggerDir(pad, 0);
            this.isDoubleTrigger = false;
//            p("! reset DOUBLE_TRIGGER on pad: " + pad);
        }

        if (getLongPressValue(pad, dirs) == EXPECTED_ON_VALUE) {
            this.isLongPress = true;
//            p("! Found LONG_PRESS on pad: " + pad);
        } else {
            if (getDoubleTriggerValue(pad, dirs) == EXPECTED_ON_VALUE) {
                this.isDoubleTrigger = true;
//                p("! DOUBLE_TRIGGER on pad: " + pad);
            } else {
                this.isFootOn = catchDoubleFootOnTrigger(getFootOnValue(pad, dirs));
            }
        }


        return true;
    }

    private int footOnCounter = 0;

    /**
     * Fix that foot on comes in from Softstep 2 times
     * @param footOnData2 the expected value from hardware which denotes state 'on'
     * @return true if a footOn is registered
     */
    private boolean catchDoubleFootOnTrigger(int footOnData2) {
        if (footOnData2 == EXPECTED_ON_VALUE) {
            footOnCounter++;
            if (footOnCounter > 2) {
                footOnCounter = 0;
//                p("! FOOT_ON on pad: ");
                return true;
            }
        }
        return false;
    }

    private int getFootOnValue(Softstep1Pad pad, Map<Integer, Integer> dirs) {
        return dirs.get(footOnDirectionsIndex(pad));
    }

    private int getDoubleTriggerValue(Softstep1Pad pad, Map<Integer, Integer> dirs) {
        return dirs.get(doubleTriggerDirectionsIndex(pad));
    }

    private int getLongPressValue(Softstep1Pad pad, Map<Integer, Integer> dirs) {
        return dirs.get(longPressDirectionsIndex(pad));
    }

    private Integer setFootOnDir(Softstep1Pad pad, int value) {
        return pad.getDirections().put(footOnDirectionsIndex(pad), value);
    }

    private Integer setDoubleTriggerDir(Softstep1Pad pad, int value) {
        return pad.getDirections().put(doubleTriggerDirectionsIndex(pad), value);
    }

    private Integer setlongPressDir(Softstep1Pad pad, int value) {
        return pad.getDirections().put(longPressDirectionsIndex(pad), value);
    }

    private int footOnDirectionsIndex(Softstep1Pad pad) {
        return pad.getMinData1() + GestureOffsets.footOn.ordinal();
    }

    private int doubleTriggerDirectionsIndex(Softstep1Pad pad) {
        return pad.getMinData1() + GestureOffsets.doubleTrigger.ordinal();
    }

    private int longPressDirectionsIndex(Softstep1Pad pad) {
        return pad.getMinData1() + GestureOffsets.longPress.ordinal();
    }

    public boolean isAnyPress() {
        return isAnyPress;
    }


}
