package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.BaseConsolePrinter;

import java.util.List;


public class UserControlls extends BaseConsolePrinter implements HasControllsForPage {

    private final Page page;
    private final ApiManager apiManager;
    private final PadConfigurationManager padConfigManager;
    
    // State tracking for toggle and increment modes
    private final boolean[] toggleStates = new boolean[10];
    private final int[] incrementValues = new int[10];

    public UserControlls(Page page, ApiManager apiManager, PadConfigurationManager padConfigManager) {
        super(apiManager.getHost());
        this.page = page;
        this.apiManager = apiManager;
        this.padConfigManager = padConfigManager;
        
        initializePadStates();
    }
    
    private void initializePadStates() {
        for (int i = 0; i < 10; i++) {
            toggleStates[i] = false;
            
            PadConfigurationManager.PadConfig config = padConfigManager.getPadConfig(i);
            incrementValues[i] = config.min;
            
            // Set initial LED states to match actual pad states
            updateInitialHardwareFeedback(i, config);
        }
    }
    
    private void updateInitialHardwareFeedback(int padIndex, PadConfigurationManager.PadConfig config) {
        // Set LED to match initial pad state based on mode
        LedStates initialLedState;
        
        switch (config.mode) {
            case TOGGLE:
                // Toggle starts OFF (false), so show TOGGLE_OFF state
                initialLedState = Page.USER_LED_STATES.TOGGLE_OFF;
                break;
                
            case INCREMENT:
                // INCREMENT starts at min value, check if next step would wrap
                int currentValue = incrementValues[padIndex];  // Should be config.min
                int stepSize = (int) config.stepSize;
                int nextValue = currentValue + stepSize;
                
                if (currentValue <= config.min) {
                    initialLedState = Page.USER_LED_STATES.INCREMENT_MIN;
                } else if (nextValue > config.max) {
                    initialLedState = Page.USER_LED_STATES.INCREMENT_MAX;  // Next will wrap
                } else {
                    initialLedState = Page.USER_LED_STATES.INCREMENT_MID;  // Safe to increment
                }
                break;
                
            case MOMENTARY:
                // MOMENTARY starts released, so show MOMENTARY_RELEASED state
                initialLedState = Page.USER_LED_STATES.MOMENTARY_RELEASED;
                break;
                
            case PRESSURE:
                // PRESSURE starts released, so show PRESSURE_RELEASED state  
                initialLedState = Page.USER_LED_STATES.PRESSURE_RELEASED;
                break;
                
            default:
                initialLedState = Page.USER_LED_STATES.DISABLED;
                break;
        }
        
        // Update hardware LED to initial state using PERF-aware method
        apiManager.getSoftstepController().updateLedStatesForPerfMode(Page.USER, padIndex, initialLedState);
    }

    @Override
    public Page getPage() {
        return this.page;
    }

    @Override
    public void processControlls(List<Softstep1Pad> pushedDownPads, ShortMidiMessage msg) {
        // First check for long press actions - these have priority
        processPadLongPress(pushedDownPads);
        
        // Process normal pad functionality
        pushedDownPads.forEach(pad -> {
            processPadWithConfiguration(pad);
            pad.notifyControlConsumed();
        });
    }
    
    private void processPadLongPress(List<Softstep1Pad> pushedDownPads) {
        // Process long press actions for each pad (only if enabled)
        pushedDownPads.stream()
                .filter(pad -> pad.gestures().isLongPress())
                .filter(pad -> {
                    PadConfigurationManager.PadConfig config = padConfigManager.getPadConfig(pad.getNumber());
                    return config.longPressEnabled; // Only process if long press is enabled
                })
                .forEach(pad -> {
                    handlePadLongPress(pad);
                    pad.gestures().clearLongPressEvent(); // Clear the long press event
                    pad.notifyControlConsumed();
                });
    }
    
    private void handlePadLongPress(Softstep1Pad pad) {
        int padIndex = pad.getNumber();
        PadConfigurationManager.PadConfig config = padConfigManager.getPadConfig(padIndex);
        
        // Send the configured long press value (always send full value - no range scaling for long press)
        int longPressValue = config.longPressValue;
        
        // Clamp to 0-127 range
        longPressValue = Math.max(0, Math.min(127, longPressValue));
        
        // Send to separate UserControl: pad 0-9 use UserControl 10-19 for long press
        int longPressUserControlIndex = padIndex + 10;
        
        // Use burst sending with global settings for consistent mapping behavior
        String description = "Hardware Long Press Pad " + padIndex;
        apiManager.getApiToHost().sendUserControlBurst(
            longPressUserControlIndex, 
            longPressValue, 
            padConfigManager.getBurstCount(), 
            padConfigManager.getBurstDelayMs(),
            description
        );
        
        // Update hardware LED to show long press was triggered (brief flash)
        updateHardwareLongPressFeedback(padIndex);
        
        // Debug logging
        double normalizedDisplay = longPressValue / 127.0;
        p(String.format("LONG PRESS Pad %d → UserControl%d: burst %d signals (value %d, %.3f normalized, configured: %d)", 
            padIndex, longPressUserControlIndex, padConfigManager.getBurstCount(), longPressValue, normalizedDisplay, config.longPressValue));
    }
    
    private void updateHardwareLongPressFeedback(int padIndex) {
        // Brief yellow flash to indicate long press was triggered using Page constants
        // Use PERF-aware LED update method for hybrid mode compatibility
        apiManager.getSoftstepController().updateLedStatesForPerfMode(Page.USER, padIndex, Page.USER_LED_STATES.LONG_PRESS_FLASH);
        
        // Restore normal LED state after brief delay (via simple timer)
        // Note: In production, you might want to use a more sophisticated timing mechanism
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                // Restore based on current state - get fresh config in case it changed
                PadConfigurationManager.PadConfig config = padConfigManager.getPadConfig(padIndex);
                int currentOutputValue = getCurrentPadOutputValue(padIndex, config);
                updateHardwareFeedback(padIndex, config, currentOutputValue);
            }
        }, 500); // 500ms flash duration
    }
    
    private int getCurrentPadOutputValue(int padIndex, PadConfigurationManager.PadConfig config) {
        // Get the current output value based on pad mode and internal state
        switch (config.mode) {
            case TOGGLE:
                return toggleStates[padIndex] ? config.max : config.min;
            case INCREMENT:
                return incrementValues[padIndex];
            case PRESSURE:
            case MOMENTARY:
            default:
                return config.min; // Default to minimum when not actively pressed
        }
    }
    
    private void processPadWithConfiguration(Softstep1Pad pad) {
        int padIndex = pad.getNumber();
        PadConfigurationManager.PadConfig config = padConfigManager.getPadConfig(padIndex);
        Gestures gestures = pad.gestures();
        
        int outputValue = 0;
        boolean sendValue = false;
        boolean updateHardware = false;
        
        switch (config.mode) {
            case PRESSURE:
                outputValue = processPressureMode(pad, config);
                sendValue = true;  // Always send pressure values
                updateHardware = true;
                break;
                
            case MOMENTARY:
                // Update hardware on state changes
                if (gestures.isFootOn() || gestures.isFootOff()) {
                    updateHardware = true;
                }
                // Always send current momentary state (pressed or not)
                outputValue = (gestures.getPressure() > 10) ? config.max : config.min;
                sendValue = true;
                break;
                
            case TOGGLE:
                if (gestures.isFootOn()) {  // Toggle state on press
                    toggleStates[padIndex] = !toggleStates[padIndex];
                    updateHardware = true;
                }
                // Always send current toggle state (like pressure mode)
                outputValue = toggleStates[padIndex] ? config.max : config.min;
                sendValue = true;
                break;
                
            case INCREMENT:
                if (gestures.isFootOn()) {  // Increment on press
                    processIncrementMode(padIndex, config);
                    updateHardware = true;
                }
                // Always send current increment value (like toggle mode)
                outputValue = incrementValues[padIndex];
                sendValue = true;
                break;
        }
        
        if (sendValue) {
            if (config.inverted) {
                outputValue = config.max + config.min - outputValue;
            }
            
            outputValue = Math.max(config.min, Math.min(config.max, outputValue));
            
            // Scale from configured range (min-max) to resolution range (0-127) for Bitwig API
            int scaledValue = 0;
            if (config.max > config.min) {
                double normalizedValue = (double)(outputValue - config.min) / (double)(config.max - config.min);
                scaledValue = (int) Math.round(normalizedValue * 127.0);
            }
            
            apiManager.getApiToHost().setValueOfUserControl(padIndex, scaledValue);
            
            // Debug logging
            double normalizedDisplay = scaledValue / 127.0;
            p(String.format("Pad %d [%s]: sent value %d (%.3f normalized, raw: %d, inverted: %s, range: %d-%d)", 
                padIndex, config.mode.toString(), scaledValue, normalizedDisplay,
                config.inverted ? (config.max + config.min - outputValue) : outputValue, 
                config.inverted, config.min, config.max));
        }
        
        if (updateHardware) {
            updateHardwareFeedback(padIndex, config, outputValue);
        }
    }
    
    private void processIncrementMode(int padIndex, PadConfigurationManager.PadConfig config) {
        int stepSize = (int) config.stepSize;
        incrementValues[padIndex] += stepSize;
        if (incrementValues[padIndex] > config.max) {
            incrementValues[padIndex] = config.min;
        }
    }
    
    private void updateHardwareFeedback(int padIndex, PadConfigurationManager.PadConfig config, int outputValue) {
        // Hardware LED feedback based on control mode and current values
        // This is called only from user interactions, not software feedback
        
        LedStates ledState;
        
        switch (config.mode) {
            case TOGGLE:
                // LED shows toggle state using Page constants
                ledState = toggleStates[padIndex] ? 
                    Page.USER_LED_STATES.TOGGLE_ON :
                    Page.USER_LED_STATES.TOGGLE_OFF;
                break;
                
            case MOMENTARY:
                // LED shows momentary state using Page constants
                ledState = (outputValue > config.min) ? 
                    Page.USER_LED_STATES.MOMENTARY_PRESSED :
                    Page.USER_LED_STATES.MOMENTARY_RELEASED;
                break;
                
            case PRESSURE:
                // LED shows pressure state using Page constants
                ledState = (outputValue > config.min) ? 
                    Page.USER_LED_STATES.PRESSURE_PRESSED :
                    Page.USER_LED_STATES.PRESSURE_RELEASED;
                break;
                
            case INCREMENT:
                // Enhanced LED logic: RED when next step will wrap around
                int currentValue = incrementValues[padIndex];
                int stepSize = (int) config.stepSize;
                int nextValue = currentValue + stepSize;
                
                if (currentValue <= config.min) {
                    // At minimum value
                    ledState = Page.USER_LED_STATES.INCREMENT_MIN;
                } else if (nextValue > config.max) {
                    // Next increment will wrap to min - show warning RED
                    ledState = Page.USER_LED_STATES.INCREMENT_MAX;
                } else {
                    // Somewhere in between, can increment safely
                    ledState = Page.USER_LED_STATES.INCREMENT_MID;
                }
                break;
                
            default:
                ledState = Page.USER_LED_STATES.DISABLED;
                break;
        }
        
        // Update hardware LED using PERF-aware method
        apiManager.getSoftstepController().updateLedStatesForPerfMode(Page.USER, padIndex, ledState);
    }
    
    private int processPressureMode(Softstep1Pad pad, PadConfigurationManager.PadConfig config) {
        double pressure = pad.gestures().getPressure();
        double scaledPressure = pressure * config.stepSize;
        scaledPressure = Math.max(0.0, Math.min(127.0, scaledPressure));
        
        int range = config.max - config.min;
        return config.min + (int) Math.round((scaledPressure / 127.0) * range);
    }
    
}
