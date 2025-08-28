package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.SimpleConsolePrinter;
import de.davidrival.softstep.hardware.LedColor;
import de.davidrival.softstep.hardware.LedLight;

import java.util.List;


public class UserControlls extends SimpleConsolePrinter implements HasControllsForPage {

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
        }
    }

    @Override
    public Page getPage() {
        return this.page;
    }

    @Override
    public void processControlls(List<Softstep1Pad> pushedDownPads, ShortMidiMessage msg) {
        // Process only the pads that are actively being touched
        pushedDownPads.forEach(pad -> {
            processPadWithConfiguration(pad);
            pad.notifyControlConsumed();
        });
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
                if (gestures.isFootOn()) {  // Only increment on press
                    outputValue = processIncrementMode(padIndex, config);
                    sendValue = true;
                    updateHardware = true;
                }
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
    
    private int processIncrementMode(int padIndex, PadConfigurationManager.PadConfig config) {
        int stepSize = (int) config.stepSize;
        incrementValues[padIndex] += stepSize;
        if (incrementValues[padIndex] > config.max) {
            incrementValues[padIndex] = config.min;
        }
        return incrementValues[padIndex];
    }
    
    private void updateHardwareFeedback(int padIndex, PadConfigurationManager.PadConfig config, int outputValue) {
        // Hardware LED feedback based on control mode and current values
        // This is called only from user interactions, not software feedback
        
        LedStates ledState;
        
        switch (config.mode) {
            case TOGGLE:
                // LED shows toggle state: RED when ON, GREEN when OFF
                ledState = toggleStates[padIndex] ? 
                    new LedStates(LedColor.RED, LedLight.ON) :
                    new LedStates(LedColor.GREEN, LedLight.ON);
                break;
                
            case MOMENTARY:
                // LED shows momentary state: RED when pressed, GREEN when released
                ledState = (outputValue > config.min) ? 
                    new LedStates(LedColor.RED, LedLight.ON) :
                    new LedStates(LedColor.GREEN, LedLight.ON);
                break;
                
            case PRESSURE:
                // LED shows pressure state: RED when pressed, GREEN when released
                ledState = (outputValue > config.min) ? 
                    new LedStates(LedColor.RED, LedLight.ON) :
                    new LedStates(LedColor.GREEN, LedLight.ON);
                break;
                
            case INCREMENT:
                // LED shows RED when max is reached, otherwise GREEN
                ledState = (incrementValues[padIndex] >= config.max) ? 
                    new LedStates(LedColor.RED, LedLight.ON) : 
                    new LedStates(LedColor.GREEN, LedLight.ON);
                break;
                
            default:
                ledState = new LedStates(LedColor.GREEN, LedLight.OFF);
                break;
        }
        
        // Update hardware LED
        apiManager.getSoftstepController().updateLedStates(Page.USER, padIndex, ledState);
    }
    
    private int processPressureMode(Softstep1Pad pad, PadConfigurationManager.PadConfig config) {
        double pressure = pad.gestures().getPressure();
        double scaledPressure = pressure * config.stepSize;
        scaledPressure = Math.max(0.0, Math.min(127.0, scaledPressure));
        
        int range = config.max - config.min;
        return config.min + (int) Math.round((scaledPressure / 127.0) * range);
    }
    
}
