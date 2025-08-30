package de.davidrival.softstep.controller;

import com.bitwig.extension.controller.api.*;

public class PadConfigurationManager {
    
    public enum PadMode {
        PRESSURE, MOMENTARY, TOGGLE, INCREMENT
    }
    
    public static class PadConfig {
        public PadMode mode = PadMode.PRESSURE;
        public int min = 0;
        public int max = 127;
        public double stepSize = 1.0;
        public boolean inverted = false;
        public boolean longPressEnabled = false;
        public int longPressValue = 0;
        
        public PadConfig() {}
        
        public PadConfig(PadMode mode, int min, int max, double stepSize, boolean inverted, boolean longPressEnabled, int longPressValue) {
            this.mode = mode;
            this.min = min;
            this.max = max;
            this.stepSize = stepSize;
            this.inverted = inverted;
            this.longPressEnabled = longPressEnabled;
            this.longPressValue = longPressValue;
        }
    }
    
    private static final int NUM_PADS = 10;
    private static final String[] MODE_OPTIONS = {"pressure", "momentary", "toggle", "increment"};
    
    private final ControllerHost host;
    private final SettableEnumValue[] padModeSettings;
    private final SettableStringValue[] padMinSettings;
    private final SettableStringValue[] padMaxSettings;
    private final SettableStringValue[] padStepSettings;
    private final SettableBooleanValue[] padInvertedSettings;
    private final SettableBooleanValue[] padLongPressEnabledSettings;
    private final SettableStringValue[] padLongPressSettings;
    
    private final PadConfig[] currentConfigs;
    
    public PadConfigurationManager(ControllerHost host) {
        this.host = host;
        this.padModeSettings = new SettableEnumValue[NUM_PADS];
        this.padMinSettings = new SettableStringValue[NUM_PADS];
        this.padMaxSettings = new SettableStringValue[NUM_PADS];
        this.padStepSettings = new SettableStringValue[NUM_PADS];
        this.padInvertedSettings = new SettableBooleanValue[NUM_PADS];
        this.padLongPressEnabledSettings = new SettableBooleanValue[NUM_PADS];
        this.padLongPressSettings = new SettableStringValue[NUM_PADS];
        this.currentConfigs = new PadConfig[NUM_PADS];
        
        setupPreferences();
        setupObservers();
    }
    
    private void setupPreferences() {
        Preferences preferences = host.getPreferences();
        
        for (int i = 0; i < NUM_PADS; i++) {
            final int padIndex = i;
            String padName = "Pad " + (i + 1);
            
            padModeSettings[i] = preferences.getEnumSetting(
                "Mode", padName, MODE_OPTIONS, MODE_OPTIONS[0]);
                
            padMinSettings[i] = preferences.getStringSetting(
                "Min Value", padName, 8, "0");
                
            padMaxSettings[i] = preferences.getStringSetting(
                "Max Value", padName, 8, "127");
                
            padStepSettings[i] = preferences.getStringSetting(
                "Step Size", padName, 8, "1");
                
            padInvertedSettings[i] = preferences.getBooleanSetting(
                "Inverted", padName, false);
                
            padLongPressEnabledSettings[i] = preferences.getBooleanSetting(
                "Long Press Enabled", padName, false);
                
            padLongPressSettings[i] = preferences.getStringSetting(
                "Long Press Value", padName, 8, "0");
            
            padModeSettings[i].markInterested();
            padMinSettings[i].markInterested();
            padMaxSettings[i].markInterested();
            padStepSettings[i].markInterested();
            padInvertedSettings[i].markInterested();
            padLongPressEnabledSettings[i].markInterested();
            padLongPressSettings[i].markInterested();
            
            currentConfigs[i] = new PadConfig();
            updateConfigFromSettings(padIndex);
        }
        
        host.println("PadConfigurationManager initialized with default settings");
    }
    
    // Custom parsing methods with validation
    private int parseIntegerValue(String value, int min, int max, int defaultValue, String fieldName, int padIndex) {
        try {
            int parsedValue = Integer.parseInt(value.trim());
            if (parsedValue < min || parsedValue > max) {
                host.showPopupNotification("Pad " + (padIndex + 1) + " " + fieldName + " must be between " + min + " and " + max + ". Using default: " + defaultValue);
                return defaultValue;
            }
            return parsedValue;
        } catch (NumberFormatException e) {
            host.showPopupNotification("Pad " + (padIndex + 1) + " " + fieldName + " invalid format: '" + value + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    private double parseDoubleValue(String value, double min, double max, double defaultValue, String fieldName, int padIndex) {
        try {
            double parsedValue = Double.parseDouble(value.trim());
            if (parsedValue < min || parsedValue > max) {
                host.showPopupNotification("Pad " + (padIndex + 1) + " " + fieldName + " must be between " + min + " and " + max + ". Using default: " + defaultValue);
                return defaultValue;
            }
            return parsedValue;
        } catch (NumberFormatException e) {
            host.showPopupNotification("Pad " + (padIndex + 1) + " " + fieldName + " invalid format: '" + value + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    private void setupObservers() {
        for (int i = 0; i < NUM_PADS; i++) {
            final int padIndex = i;
            
            padModeSettings[i].addValueObserver(value -> {
                updateConfigFromSettings(padIndex);
                host.println("Pad " + (padIndex + 1) + " mode changed to: " + value);
            });
            
            padMinSettings[i].addValueObserver(value -> {
                updateConfigFromSettings(padIndex);
                host.println("Pad " + (padIndex + 1) + " min value changed to: '" + value + "'");
            });
            
            padMaxSettings[i].addValueObserver(value -> {
                updateConfigFromSettings(padIndex);
                host.println("Pad " + (padIndex + 1) + " max value changed to: '" + value + "'");
            });
            
            padStepSettings[i].addValueObserver(value -> {
                updateConfigFromSettings(padIndex);
                host.println("Pad " + (padIndex + 1) + " step size changed to: '" + value + "'");
            });
            
            padInvertedSettings[i].addValueObserver(value -> {
                updateConfigFromSettings(padIndex);
                host.println("Pad " + (padIndex + 1) + " inverted changed to: " + value);
            });
            
            padLongPressEnabledSettings[i].addValueObserver(value -> {
                updateConfigFromSettings(padIndex);
                host.println("Pad " + (padIndex + 1) + " long press enabled changed to: " + value);
            });
            
            padLongPressSettings[i].addValueObserver(value -> {
                updateConfigFromSettings(padIndex);
                host.println("Pad " + (padIndex + 1) + " long press value changed to: '" + value + "'");
            });
        }
    }
    
    private void updateConfigFromSettings(int padIndex) {
        if (padIndex < 0 || padIndex >= NUM_PADS) return;
        
        PadConfig config = currentConfigs[padIndex];
        
        String modeString = padModeSettings[padIndex].get();
        switch (modeString) {
            case "pressure":
                config.mode = PadMode.PRESSURE;
                break;
            case "momentary":
                config.mode = PadMode.MOMENTARY;
                break;
            case "toggle":
                config.mode = PadMode.TOGGLE;
                break;
            case "increment":
                config.mode = PadMode.INCREMENT;
                break;
            default:
                config.mode = PadMode.PRESSURE;
                break;
        }
        
        // Parse string values with validation
        String minString = padMinSettings[padIndex].get();
        String maxString = padMaxSettings[padIndex].get();
        String stepString = padStepSettings[padIndex].get();
        String longPressString = padLongPressSettings[padIndex].get();
        
        config.min = parseIntegerValue(minString, 0, 127, 0, "Min Value", padIndex);
        config.max = parseIntegerValue(maxString, 0, 127, 127, "Max Value", padIndex);
        // Step Size validation depends on mode context:
        // - INCREMENT mode: integer 1-64 (increment step)  
        // - PRESSURE mode: double 0.1-10.0 (pressure multiplier)
        // For now, use wider range to support both
        config.stepSize = parseDoubleValue(stepString, 0.1, 64.0, 1.0, "Step Size", padIndex);
        config.inverted = padInvertedSettings[padIndex].get();
        config.longPressEnabled = padLongPressEnabledSettings[padIndex].get();
        config.longPressValue = parseIntegerValue(longPressString, 0, 127, 0, "Long Press Value", padIndex);
        
        // Debug logging
        host.println("DEBUG: Pad " + padIndex + " config updated - min:" + config.min + 
                    " max:" + config.max + " stepSize:" + config.stepSize + 
                    " mode:" + config.mode + " inverted:" + config.inverted +
                    " longPressEnabled:" + config.longPressEnabled + " longPressValue:" + config.longPressValue);
        
        if (config.min >= config.max) {
            config.max = config.min + 1;
            padMaxSettings[padIndex].set(String.valueOf(config.max));
        }
    }
    
    public PadConfig getPadConfig(int padIndex) {
        if (padIndex < 0 || padIndex >= NUM_PADS) {
            return new PadConfig();
        }
        return currentConfigs[padIndex];
    }
    
    public PadConfig[] getAllPadConfigs() {
        return currentConfigs.clone();
    }
    
    public void resetPadToDefaults(int padIndex) {
        if (padIndex < 0 || padIndex >= NUM_PADS) return;
        
        padModeSettings[padIndex].set(MODE_OPTIONS[0]);
        padMinSettings[padIndex].set("0");
        padMaxSettings[padIndex].set("127");
        padStepSettings[padIndex].set("1.0");
        padInvertedSettings[padIndex].set(false);
        
        host.println("Pad " + (padIndex + 1) + " reset to default settings");
    }
    
    public void resetAllPadsToDefaults() {
        for (int i = 0; i < NUM_PADS; i++) {
            resetPadToDefaults(i);
        }
        host.println("All pads reset to default settings");
    }
}