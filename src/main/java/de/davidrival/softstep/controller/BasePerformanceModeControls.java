package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.BaseConsolePrinter;
import de.davidrival.softstep.debug.DebugLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Base class for Performance modes (PERF and PERF2) that share common upper row functionality.
 * 
 * Common Upper Row (Pads 4-9):
 * - Pad 4: BWS Track Cycle
 * - Pad 5: Mute/Stop Clip (CLIP mode functionality)
 * - Pad 6: Record Arm/Delete All Clips (CLIP mode functionality)  
 * - Pads 7-9: User Controls (USER mode functionality)
 * 
 * The lower row (Pads 0-3) is mode-specific and handled by subclasses.
 */
public abstract class BasePerformanceModeControls extends BaseConsolePrinter implements HasControllsForPage, BwsTrackDiscoveryService.LedUpdateCallback {
    
    // Common pad assignments for upper row
    protected static final int TRACK_CYCLE_PAD = 4;  // BWS track cycling
    protected static final int MUTE_PAD = 5;          // Mute/stop clip (CLIP functionality)
    protected static final int ARM_PAD = 6;           // Record arm/delete all (CLIP functionality) 
    // Pads 7-9: User controls (handled by isUserPad method)
    
    protected final Page page;
    protected final ApiManager apiManager;
    protected final PadConfigurationManager padConfigManager;
    
    // Common functionality instances
    protected final ClipControls clipControls;
    protected final UserControls userControls;
    
    public BasePerformanceModeControls(Page page, ApiManager apiManager, PadConfigurationManager padConfigManager) {
        super(apiManager.getHost());
        this.page = page;
        this.apiManager = apiManager;
        this.padConfigManager = padConfigManager;
        
        // Create instances of existing functionality for upper row
        this.clipControls = new ClipControls(Page.CLIP, apiManager);
        this.userControls = new UserControls(Page.USER, apiManager, padConfigManager);
        
        // Set up BWS LED callback for track selection feedback
        BwsTrackDiscoveryService bwsService = apiManager.getBwsTrackDiscoveryService();
        if (bwsService != null) {
            bwsService.setLedUpdateCallback(this);
            DebugLogger.common(apiManager.getHost(), padConfigManager, 
                page.name() + ": BWS LED callback registered");
        }
        
        // Set initial BWS LED state
        updateInitialBwsLed();
    }
    
    @Override
    public Page getPage() {
        return this.page;
    }
    
    @Override
    public void refreshLedStates() {
        // Refresh common LED states
        clipControls.refreshLedStates();
        userControls.refreshLedStates();
        
        // Allow subclasses to refresh their specific LED states
        refreshModeSpecificLedStates();
    }
    
    @Override
    public void processControlls(List<Softstep1Pad> pushedDownPads, ShortMidiMessage msg) {
        // Split pads into different functional groups
        List<Softstep1Pad> clipPads = new ArrayList<>();      // Upper row: Pads 5-6 (mute/arm)
        List<Softstep1Pad> userPads = new ArrayList<>();      // Upper row: Pads 7-9 
        List<Softstep1Pad> trackCyclePads = new ArrayList<>(); // Upper row: Pad 4
        List<Softstep1Pad> modeSpecificPads = new ArrayList<>(); // Lower row: Pads 0-3 (mode-specific)
        
        for (Softstep1Pad pad : pushedDownPads) {
            int padIndex = pad.getNumber();
            
            if (padIndex == TRACK_CYCLE_PAD) {
                trackCyclePads.add(pad);
            } else if (isClipPad(padIndex)) {
                clipPads.add(pad);
            } else if (isUserPad(padIndex)) {
                userPads.add(pad);
            } else {
                // Lower row (Pads 0-3) - mode-specific handling
                modeSpecificPads.add(pad);
            }
        }
        
        // Route upper row to appropriate subsystems
        if (!clipPads.isEmpty()) {
            clipControls.processControlls(clipPads, msg);
        }
        
        if (!userPads.isEmpty()) {
            userControls.processControlls(userPads, msg);
        }
        
        if (!trackCyclePads.isEmpty()) {
            processTrackCyclePads(trackCyclePads, msg);
        }
        
        // Route lower row to mode-specific handling
        if (!modeSpecificPads.isEmpty()) {
            processModeSpecificPads(modeSpecificPads, msg);
        }
    }
    
    /**
     * Determines if a pad should use CLIP mode functionality.
     * Common upper row CLIP pads: 5 (mute), 6 (arm)
     */
    protected boolean isClipPad(int padIndex) {
        return padIndex == MUTE_PAD || padIndex == ARM_PAD;
    }
    
    /**
     * Determines if a pad should use USER mode functionality.
     * Common upper row USER pads: 7, 8, 9
     */
    protected boolean isUserPad(int padIndex) {
        return padIndex >= 7 && padIndex <= 9;
    }
    
    /**
     * Handles BWS track cycling functionality (Pad 4).
     * Common to both PERF and PERF2 modes.
     */
    protected void processTrackCyclePads(List<Softstep1Pad> trackCyclePads, ShortMidiMessage msg) {
        for (Softstep1Pad pad : trackCyclePads) {
            if (pad.gestures().isFootOn()) {
                // Cycle to next BWS track
                BwsTrackDiscoveryService bwsService = apiManager.getBwsTrackDiscoveryService();
                if (bwsService != null) {
                    bwsService.cycleToNextBwsTrack();
                    DebugLogger.perf(apiManager.getHost(), padConfigManager, 
                        page.name() + ": BWS track cycle triggered");
                } else {
                    DebugLogger.common(apiManager.getHost(), padConfigManager, 
                        page.name() + ": BWS service not available for track cycling");
                }
                
                pad.notifyControlConsumed();
            }
        }
    }
    
    /**
     * Updates initial BWS LED state after service initialization.
     * Common to both PERF and PERF2 modes.
     */
    protected void updateInitialBwsLed() {
        // Delay the initial LED update to ensure BWS discovery is complete
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                BwsTrackDiscoveryService bwsService = apiManager.getBwsTrackDiscoveryService();
                
                if (bwsService != null && bwsService.isInitialized()) {
                    int currentSlot = bwsService.getCurrentBwsSlot();
                    DebugLogger.common(apiManager.getHost(), padConfigManager, String.format(
                        "%s: Initial BWS LED update - current slot: %d", page.name(), currentSlot));
                    updateBwsLed(currentSlot);
                }
            }
        }, 3000); // 3 second delay to ensure BWS discovery is complete
    }
    
    /**
     * BWS LED callback implementation.
     * Updates Pad 4 LED based on current BWS track state.
     */
    @Override
    public void updateBwsLed(int bwsSlot) {
        LedStates ledState;
        
        switch (bwsSlot) {
            case 0: ledState = Page.USER_LED_STATES.BWS_TRACK_0; break;  // YELLOW + ON
            case 1: ledState = Page.USER_LED_STATES.BWS_TRACK_1; break;  // YELLOW + BLINK  
            case 2: ledState = Page.USER_LED_STATES.BWS_TRACK_2; break;  // YELLOW + FAST_BLINK
            case 3: ledState = Page.USER_LED_STATES.BWS_TRACK_3; break;  // RED + ON
            case 4: ledState = Page.USER_LED_STATES.BWS_TRACK_4; break;  // RED + BLINK
            case 5: ledState = Page.USER_LED_STATES.BWS_TRACK_5; break;  // RED + FAST_BLINK
            case -2: ledState = Page.USER_LED_STATES.BWS_NON_BWS_TRACK; break;  // GREEN + BLINK (non-BWS track)
            default: ledState = Page.USER_LED_STATES.BWS_INACTIVE; break;  // GREEN + OFF (no BWS tracks)
        }
        
        // Update LED using current page context
        apiManager.getSoftstepController().updateLedStatesForPerfMode(page, TRACK_CYCLE_PAD, ledState);
        
        DebugLogger.perf(apiManager.getHost(), padConfigManager, String.format(
            "%s: BWS LED Callback - Updated PAD%d to show BWS:%d state (%s)", 
            page.name(), TRACK_CYCLE_PAD, bwsSlot, ledState.toString()));
    }
    
    // Abstract methods for mode-specific functionality
    
    /**
     * Handle mode-specific pads (lower row: Pads 0-3).
     * PERF mode: Standard clip slots 0-3
     * PERF2 mode: Focused clip (0), Smart assistant (1), User controls (2-3)
     */
    protected abstract void processModeSpecificPads(List<Softstep1Pad> modeSpecificPads, ShortMidiMessage msg);
    
    /**
     * Refresh mode-specific LED states.
     * Called during refreshLedStates() to allow subclasses to update their specific LEDs.
     */
    protected abstract void refreshModeSpecificLedStates();
}