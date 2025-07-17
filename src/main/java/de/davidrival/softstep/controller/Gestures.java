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
    
    private boolean isFootOff = false;
    
    private int previousMaxPressure = 0;
    
    private boolean hasAlreadyFired = false;



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
        isFootOff = false;
        previousMaxPressure = 0;
        hasAlreadyFired = false;
        setFootOnDir(pad, -1);
        setlongPressDir(pad, -1);
        setDoubleTriggerDir(pad, -1);
//        p("reset pad: " + pad);
    }

    public boolean set(Softstep1Pad pad) {
        Map<Integer, Integer> dirs = pad.getDirections();

        this.pressure = dirs.get(pad.getMinData1() + GestureOffsets.pressure.ordinal());

        // Simple logic: any direction > 0 = foot on, all directions = 0 = foot off
        boolean currentFootState = dirs.values().stream().anyMatch(value -> value > 0);
        
        // Edge detection for foot on/off (single-fire events)
        boolean footOnEdge = false;
        boolean previousFootState = (previousMaxPressure > 0);
        
        if (!previousFootState && currentFootState && !hasAlreadyFired) {
            // Rising edge: foot pressed down (OFF → ON) and hasn't fired yet
            footOnEdge = true;
            this.isFootOff = false;
            hasAlreadyFired = true;
        } else if (previousFootState && !currentFootState) {
            // Falling edge: foot lifted up (ON → OFF)
            footOnEdge = false;
            this.isFootOff = true;
            hasAlreadyFired = false; // Reset for next press
        } else {
            // No transition or already fired
            footOnEdge = false;
            this.isFootOff = false;
        }
        
        // Update previous state (1 if any direction > 0, 0 if all are 0)
        previousMaxPressure = currentFootState ? 1 : 0;
        
        // For backward compatibility
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
                // Use edge detection for clean single-fire foot on events
                this.isFootOn = footOnEdge;
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
    
    public boolean isFootOff() {
        return isFootOff;
    }


}
