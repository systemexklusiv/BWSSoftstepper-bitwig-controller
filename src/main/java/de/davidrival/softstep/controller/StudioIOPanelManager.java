package de.davidrival.softstep.controller;

import com.bitwig.extension.controller.api.*;
import de.davidrival.softstep.api.ApiManager;

/**
 * Manages the Studio I/O Panel controls for long press testing.
 * This class creates UI controls that appear in Bitwig's Studio I/O Panel
 * (at the top of the main window) to trigger long press UserControls for easy mapping.
 */
public class StudioIOPanelManager {
    
    private static final int NUM_PADS = 10;
    private static final String TRIGGER_VALUE = "Trigger";
    private static final String IDLE_VALUE = "Ready";
    
    private final ControllerHost host;
    private final ApiManager apiManager;
    private final PadConfigurationManager padConfigManager;
    private final DocumentState documentState;
    private final SettableEnumValue[] longPressTestButtons;
    private final boolean[] initializationComplete;
    
    public StudioIOPanelManager(ControllerHost host, ApiManager apiManager, PadConfigurationManager padConfigManager) {
        this.host = host;
        this.apiManager = apiManager;
        this.padConfigManager = padConfigManager;
        this.documentState = host.getDocumentState();
        this.longPressTestButtons = new SettableEnumValue[NUM_PADS];
        this.initializationComplete = new boolean[NUM_PADS];
        
        createStudioIOPanelControls();
        setupObservers();
    }
    
    /**
     * Creates the Studio I/O Panel controls for long press testing.
     * These appear at the top of Bitwig's main window when the controller is active.
     */
    private void createStudioIOPanelControls() {
        for (int i = 0; i < NUM_PADS; i++) {
            final int padIndex = i;
            String padName = "Pad " + padIndex; // Use 0-based indexing for pad names
            
            // Create trigger button for each long press UserControl
            longPressTestButtons[i] = documentState.getEnumSetting(
                "Assign Longpress " + padIndex, 
                padName, 
                new String[]{IDLE_VALUE, TRIGGER_VALUE}, 
                IDLE_VALUE
            );
            
            // Mark as interested to receive updates
            longPressTestButtons[i].markInterested();
            
            // Initialize as not ready yet (prevents startup triggering)
            initializationComplete[i] = false;
        }
        
        host.println("StudioIOPanelManager: Created " + NUM_PADS + " long press test controls in Studio I/O Panel");
    }
    
    /**
     * Sets up observers to handle trigger button presses.
     */
    private void setupObservers() {
        for (int i = 0; i < NUM_PADS; i++) {
            final int padIndex = i;
            
            longPressTestButtons[i].addValueObserver(value -> {
                // Only trigger if initialization is complete (prevents startup bursts)
                if (TRIGGER_VALUE.equals(value) && initializationComplete[padIndex]) {
                    triggerLongPressUserControl(padIndex);
                    
                    // Reset back to "Ready" state after triggering
                    // Use a small delay to ensure the trigger is processed first
                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            longPressTestButtons[padIndex].set(IDLE_VALUE);
                        }
                    }, 100); // 100ms delay
                }
            });
            
            // Mark initialization as complete after a short delay
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    initializationComplete[padIndex] = true;
                }
            }, 1000); // 1 second delay to ensure everything is properly initialized
        }
    }
    
    /**
     * Triggers the long press UserControl for the specified pad.
     * This allows users to easily map long press functions without needing
     * to perform actual long press gestures on the hardware.
     * 
     * Sends the UserControl value multiple times in rapid succession to ensure
     * Bitwig's mapping mode can detect it as a continuous control signal.
     * 
     * @param padIndex The pad index (0-9)
     */
    private void triggerLongPressUserControl(int padIndex) {
        PadConfigurationManager.PadConfig config = padConfigManager.getPadConfig(padIndex);
        
        // Only trigger if long press is enabled for this pad
        if (!config.longPressEnabled) {
            host.showPopupNotification("Pad " + padIndex + " long press is disabled");
            return;
        }
        
        // Calculate the long press UserControl index (pad + 10)
        int longPressUserControlIndex = padIndex + 10;
        
        // Get the configured long press value (0-127 range)
        int longPressValue = Math.max(0, Math.min(127, config.longPressValue));
        
        // Send repeated signals for mapping detection using global burst settings
        String description = "Studio I/O Panel Pad " + padIndex;
        apiManager.getApiToHost().sendUserControlBurst(
            longPressUserControlIndex, 
            longPressValue, 
            padConfigManager.getBurstCount(),
            padConfigManager.getBurstDelayMs(),
            description
        );
        
        // Debug logging
        host.println(String.format(
            "StudioIOPanelManager: Triggered long press burst for Pad %d → UserControl%d with value %d",
            padIndex, longPressUserControlIndex, longPressValue
        ));
    }
    
    /**
     * Updates the visibility/availability of long press test controls based on
     * current pad configurations. Called when pad settings change.
     */
    public void updateControlAvailability() {
        // Note: DocumentState controls are always visible, but we could add
        // additional logic here to show/hide controls based on configuration
        // if needed in the future
        
        host.println("StudioIOPanelManager: Updated control availability");
    }
    
    /**
     * Gets information about the Studio I/O Panel setup for debugging.
     * 
     * @return Information string about the current setup
     */
    public String getSetupInfo() {
        int enabledLongPressCount = 0;
        
        for (int i = 0; i < NUM_PADS; i++) {
            PadConfigurationManager.PadConfig config = padConfigManager.getPadConfig(i);
            if (config.longPressEnabled) {
                enabledLongPressCount++;
            }
        }
        
        return String.format(
            "StudioIOPanelManager: %d test controls created, %d pads have long press enabled",
            NUM_PADS, enabledLongPressCount
        );
    }
}