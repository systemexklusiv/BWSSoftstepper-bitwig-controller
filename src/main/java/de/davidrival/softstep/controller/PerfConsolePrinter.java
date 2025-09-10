package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.BaseConsolePrinter;
import de.davidrival.softstep.debug.DebugLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance Page - A hybrid mode that combines CLIP and USER functionality with BWS track cycling.
 * 
 * Layout:
 * - Pads 0-3: CLIP mode functionality (clip slots)
 * - Pad 4: TRACK_CYCLE functionality (BWS track navigation) ✨ NEW
 * - Pad 5: CLIP mode functionality (track control - mute toggle/clip stop)
 * - Pads 6-9: USER mode functionality (configurable UserControls)
 * 
 * This allows musicians to trigger/record clips, navigate BWS tracks, and use freely assignable UserControls.
 */
public class PerfConsolePrinter extends BaseConsolePrinter implements HasControllsForPage, BwsTrackDiscoveryService.LedUpdateCallback {
    
    private static final int TRACK_CYCLE_PAD = 4;  // PAD4 is dedicated to TRACK_CYCLE functionality in PERF mode
    
    private final Page page;
    private final ClipControls clipControls;
    private final UserControlls userControls;
    private final ApiManager apiManager;
    private final PadConfigurationManager padConfigManager;
    
    public PerfConsolePrinter(Page page, ApiManager apiManager, PadConfigurationManager padConfigManager) {
        super(apiManager.getHost());
        this.page = page;
        this.apiManager = apiManager;
        this.padConfigManager = padConfigManager;
        
        // Create instances of existing functionality
        this.clipControls = new ClipControls(Page.CLIP, apiManager);
        this.userControls = new UserControlls(Page.USER, apiManager, padConfigManager);
        
        // Set up BWS LED callback for track selection feedback
        BwsTrackDiscoveryService bwsService = apiManager.getBwsTrackDiscoveryService();
        if (bwsService != null) {
            bwsService.setLedUpdateCallback(this);
            apiManager.getHost().println("PerfPage: BWS LED callback registered");
        }
        
        // Set initial BWS LED state
        updateInitialBwsLed();
        
        apiManager.getHost().println("PerfPage: Initialized hybrid CLIP+USER performance mode");
    }
    
    @Override
    public Page getPage() {
        return page;
    }
    
    @Override
    public void refreshLedStates() {
        // PERF mode is hybrid of CLIP and USER - refresh both
        clipControls.refreshLedStates();
        userControls.refreshLedStates();
        
        // Also refresh BWS track cycle LED (pad 4)
        refreshBwsLed();
    }
    
    @Override
    public void processControlls(List<Softstep1Pad> pushedDownPads, ShortMidiMessage msg) {
        //apiManager.getHost().println(String.format("PerfPage: Processing %d pads", pushedDownPads.size()));
        
        // Split pads into CLIP, USER, and TRACK_CYCLE groups
        List<Softstep1Pad> clipPads = new ArrayList<>();
        List<Softstep1Pad> userPads = new ArrayList<>();
        List<Softstep1Pad> trackCyclePads = new ArrayList<>();
        
        for (Softstep1Pad pad : pushedDownPads) {
            int padIndex = pad.getNumber();
            
            if (padIndex == TRACK_CYCLE_PAD) {
                // TRACK_CYCLE_PAD = TRACK_CYCLE in PERF mode
                trackCyclePads.add(pad);
            } else if (isClipPad(padIndex)) {
                clipPads.add(pad);
            } else {
                userPads.add(pad);
            }
        }
        
        // Route to appropriate subsystems
        if (!clipPads.isEmpty()) {
            clipControls.processControlls(clipPads, msg);
        }
        
        if (!userPads.isEmpty()) {
            userControls.processControlls(userPads, msg);
        }
        
        // Handle TRACK_CYCLE pads (PAD4 only)
        if (!trackCyclePads.isEmpty()) {
            processTrackCyclePads(trackCyclePads, msg);
        }
    }
    
    /**
     * Processes TRACK_CYCLE pads (PAD4 only in PERF mode).
     * Handles BWS track cycling navigation on pad press.
     * 
     * @param trackCyclePads List of TRACK_CYCLE pads that were pressed
     * @param msg The MIDI message
     */
    private void processTrackCyclePads(List<Softstep1Pad> trackCyclePads, ShortMidiMessage msg) {
        for (Softstep1Pad pad : trackCyclePads) {
            int padIndex = pad.getNumber();
            Gestures gestures = pad.gestures();
            
            // Only log when there's actual pad activity (not idle state)
            if (gestures.getPressure() > 0 || gestures.isFootOn() || gestures.isFootOff()) {
                DebugLogger.perf(apiManager.getHost(), padConfigManager, String.format("PERF Mode: Processing TRACK_CYCLE PAD%d (pressure: %d, footOn: %s, footOff: %s)", 
                    padIndex, gestures.getPressure(), gestures.isFootOn(), gestures.isFootOff()));
            }
            
            // Only cycle on pad press (not release)
            if (gestures.isFootOn()) {
                DebugLogger.perf(apiManager.getHost(), padConfigManager, String.format("PERF Mode: PAD%d PRESSED - attempting BWS track cycle", padIndex));
                
                BwsTrackDiscoveryService bwsService = apiManager.getBwsTrackDiscoveryService();
                
                if (bwsService != null && bwsService.isInitialized()) {
                    DebugLogger.perf(apiManager.getHost(), padConfigManager, String.format("PERF Mode: BWS Service available, found %d BWS tracks", 
                        bwsService.getBwsTrackCount()));
                    
                    boolean cycled = bwsService.cycleToNextBwsTrack();
                    
                    if (cycled) {
                        int currentSlot = bwsService.getCurrentBwsSlot();
                        DebugLogger.perf(apiManager.getHost(), padConfigManager, String.format("PERF Mode: PAD%d triggered BWS track cycle SUCCESS - current slot: %d", padIndex, currentSlot));
                        updateTrackCycleLed(padIndex, currentSlot);
                    } else {
                        DebugLogger.perf(apiManager.getHost(), padConfigManager, String.format("PERF Mode: PAD%d - No BWS tracks available for cycling", padIndex));
                        updateTrackCycleLed(padIndex, -1); // No BWS tracks
                    }
                } else {
                    DebugLogger.perf(apiManager.getHost(), padConfigManager, String.format("PERF Mode: PAD%d - BWS Discovery not initialized or null", padIndex));
                }
                
                // Mark pad as consumed
                pad.notifyControlConsumed();
            }
        }
    }
    
    /**
     * Updates LED feedback for TRACK_CYCLE pad based on current BWS slot.
     * 
     * @param padIndex The pad index (should be 4)
     * @param currentBwsSlot The current BWS slot (0-5), or -1 if no BWS tracks
     */
    private void updateTrackCycleLed(int padIndex, int currentBwsSlot) {
        LedStates ledState;
        
        switch (currentBwsSlot) {
            case 0:
                ledState = Page.USER_LED_STATES.BWS_TRACK_0;  // YELLOW + ON
                break;
            case 1:
                ledState = Page.USER_LED_STATES.BWS_TRACK_1;  // YELLOW + BLINK
                break;
            case 2:
                ledState = Page.USER_LED_STATES.BWS_TRACK_2;  // YELLOW + FAST_BLINK
                break;
            case 3:
                ledState = Page.USER_LED_STATES.BWS_TRACK_3;  // RED + ON
                break;
            case 4:
                ledState = Page.USER_LED_STATES.BWS_TRACK_4;  // RED + BLINK
                break;
            case 5:
                ledState = Page.USER_LED_STATES.BWS_TRACK_5;  // RED + FAST_BLINK
                break;
            default:
                ledState = Page.USER_LED_STATES.BWS_INACTIVE;  // GREEN + OFF (no BWS tracks)
                break;
        }
        
        // Update LED using PERF-aware method
        apiManager.getSoftstepController().updateLedStatesForPerfMode(Page.PERF, padIndex, ledState);
    }
    
    /**
     * Determines if a pad should use CLIP mode functionality.
     * CLIP pads: 0, 1, 2, 3, 5
     * USER pads: 6, 7, 8, 9
     * TRACK_CYCLE pads: 4
     * 
     * @param padIndex The pad index (0-9)
     * @return true if this pad should use CLIP functionality
     */
    private boolean isClipPad(int padIndex) {
        return padIndex <= 3 || padIndex == 5;
    }
    
    /**
     * Updates initial BWS LED state after service initialization.
     * Called during PerfConsolePrinter construction.
     */
    private void updateInitialBwsLed() {
        // Delay the initial LED update to allow BWS service to complete discovery
        apiManager.getHost().scheduleTask(() -> {
            BwsTrackDiscoveryService bwsService = apiManager.getBwsTrackDiscoveryService();
            
            if (bwsService != null && bwsService.isInitialized()) {
                int currentSlot = bwsService.getCurrentBwsSlot();
                apiManager.getHost().println(String.format("PERF Mode: Initial BWS LED update - current slot: %d", currentSlot));
                updateTrackCycleLed(TRACK_CYCLE_PAD, currentSlot);
            }
        }, 3000); // 3 second delay to ensure BWS discovery is complete
    }
    
    /**
     * Refreshes the BWS track cycle LED (pad 4) immediately.
     * Used during mode switching to ensure LED reflects current BWS state.
     */
    private void refreshBwsLed() {
        BwsTrackDiscoveryService bwsService = apiManager.getBwsTrackDiscoveryService();
        
        if (bwsService != null && bwsService.isInitialized()) {
            int currentSlot = bwsService.getCurrentBwsSlot();
            apiManager.getHost().println(String.format("PERF Mode: Refreshing BWS LED - current slot: %d", currentSlot));
            updateTrackCycleLed(TRACK_CYCLE_PAD, currentSlot);
        } else {
            // BWS service not initialized - show inactive state
            apiManager.getHost().println("PERF Mode: BWS service not initialized - showing inactive LED");
            updateTrackCycleLed(TRACK_CYCLE_PAD, -1);
        }
    }
    
    /**
     * Implements BWS LED callback interface.
     * Called by BWS service when track selection changes.
     */
    @Override
    public void updateBwsLed(int bwsSlot) {
        LedStates ledState;
        
        switch (bwsSlot) {
            case 0:
                ledState = Page.USER_LED_STATES.BWS_TRACK_0;  // YELLOW + ON
                break;
            case 1:
                ledState = Page.USER_LED_STATES.BWS_TRACK_1;  // YELLOW + BLINK
                break;
            case 2:
                ledState = Page.USER_LED_STATES.BWS_TRACK_2;  // YELLOW + FAST_BLINK
                break;
            case 3:
                ledState = Page.USER_LED_STATES.BWS_TRACK_3;  // RED + ON
                break;
            case 4:
                ledState = Page.USER_LED_STATES.BWS_TRACK_4;  // RED + BLINK
                break;
            case 5:
                ledState = Page.USER_LED_STATES.BWS_TRACK_5;  // RED + FAST_BLINK
                break;
            case -2:
                ledState = Page.USER_LED_STATES.BWS_NON_BWS_TRACK;  // GREEN + BLINK (non-BWS track)
                break;
            default:
                ledState = Page.USER_LED_STATES.BWS_INACTIVE;  // GREEN + OFF (no BWS tracks)
                break;
        }
        
        // Update LED using PERF-aware method
        apiManager.getSoftstepController().updateLedStatesForPerfMode(Page.PERF, TRACK_CYCLE_PAD, ledState);
        
        DebugLogger.perf(apiManager.getHost(), padConfigManager, String.format("BWS LED Callback: Updated PAD%d to show BWS:%d state (%s)", 
            TRACK_CYCLE_PAD, bwsSlot, ledState.toString()));
    }
    
}