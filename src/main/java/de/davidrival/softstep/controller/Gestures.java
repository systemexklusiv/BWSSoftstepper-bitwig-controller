package de.davidrival.softstep.controller;

import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.api.BaseConsolePrinter;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Getter
@Setter
@ToString
public class Gestures extends BaseConsolePrinter {

    public enum GestureOffsets {
        pressure, footOn, doubleTrigger, longPress
    }
    
    public enum PadState {
        IDLE,           // All corners below threshold
        INITIAL_PRESS,  // First press detected - can fire actions
        HELD,          // Foot held down - prevents retriggering
        RELEASING      // Transitioning back to IDLE
    }
    
    // Pressure profile for 4-corner analysis
    public static class PressureProfile {
        public final int maxCorner;
        public final int totalPressure;
        public final int activeCorners;
        
        public PressureProfile(Map<Integer, Integer> directions) {
            this.maxCorner = directions.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
            this.totalPressure = directions.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
            this.activeCorners = (int) directions.values().stream()
                .mapToInt(Integer::intValue)
                .filter(v -> v > 5)  // Count corners with significant pressure
                .count();
        }
        
        public boolean isValidPress() {
            return maxCorner > 10 && activeCorners >= 1;
        }
        
        public boolean isCleanRelease() {
            return maxCorner < 5 && totalPressure < 20;
        }
    }

    // Clean state machine variables
    private PadState currentState = PadState.IDLE;
    private int currentPressure = 0;  // Current max pressure from all corners
    
    // Events that can be detected
    private boolean footOnEvent = false;      // Single-fire press event
    private boolean longPressEvent = false;  // Long press detected
    private boolean footOffEvent = false;    // Single-fire release event
    
    // Timer for long press detection
    private Timer longPressTimer = null;
    private static final int LONG_PRESS_DELAY_MS = 900; // 1.5 seconds

    public Gestures(ControllerHost hostOrNull) {
        super(hostOrNull);
    }

    public boolean set(Softstep1Pad pad) {
        Map<Integer, Integer> dirs = pad.getDirections();
        
        // Clear previous events
        clearEvents();
        
        // Analyze pressure from all 4 corners
        PressureProfile profile = new PressureProfile(dirs);
        
        // Update current pressure for UserControlls
        currentPressure = profile.maxCorner;
        
        // Update state machine and generate events
        PadState nextState = updateStateMachine(profile);
        currentState = nextState;
        
        return true;
    }
    
    private void clearEvents() {
        footOnEvent = false;
        footOffEvent = false;
        // Don't clear longPressEvent here - it should persist until consumed
    }
    
    private PadState updateStateMachine(PressureProfile profile) {
        switch (currentState) {
            case IDLE:
                if (profile.isValidPress()) {
                    footOnEvent = true;  // Fire press event
                    startLongPressTimer();
                    return PadState.INITIAL_PRESS;
                }
                return PadState.IDLE;
                
            case INITIAL_PRESS:
                if (profile.isCleanRelease()) {
                    footOffEvent = true;  // Fire release event
                    cancelLongPressTimer();
                    return PadState.IDLE;  // Direct transition to IDLE
                }
                // Move to HELD to prevent re-triggering
                return PadState.HELD;
                
            case HELD:
                if (profile.isCleanRelease()) {
                    footOffEvent = true;  // Fire release event
                    cancelLongPressTimer();
                    return PadState.IDLE;  // Direct transition to IDLE
                }
                return PadState.HELD;
                
            case RELEASING:
                // This state is no longer needed with clean release detection
                return PadState.IDLE;
                
            default:
                return PadState.IDLE;
        }
    }
    
    private void startLongPressTimer() {
        cancelLongPressTimer();  // Cancel any existing timer
        
        longPressTimer = new Timer();
        longPressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                longPressEvent = true;  // Set long press event
            }
        }, LONG_PRESS_DELAY_MS);
    }
    
    private void cancelLongPressTimer() {
        if (longPressTimer != null) {
            longPressTimer.cancel();
            longPressTimer = null;
        }
    }
    
    // Public getters for events
    public boolean isFootOn() {
        return footOnEvent;
    }
    
    public boolean isLongPress() {
        return longPressEvent;
    }
    
    public boolean isFootOff() {
        return footOffEvent;
    }
    
    public int getPressure() {
        return currentPressure;
    }
    
    // Method to clear long press event after it's been consumed
    public void clearLongPressEvent() {
        longPressEvent = false;
    }

}
