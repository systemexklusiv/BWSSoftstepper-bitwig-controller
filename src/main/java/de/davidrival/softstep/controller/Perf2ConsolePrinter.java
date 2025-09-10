package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.*;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.BaseConsolePrinter;
import de.davidrival.softstep.debug.DebugLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * PERF2 Mode - Smart Recording Workflow
 * 
 * This mode revolutionizes loop recording by providing intelligent clip slot management.
 * Instead of managing multiple clip slots manually, it focuses on a single "cursor" slot
 * with smart navigation and recording assistance.
 * 
 * Layout:
 * - Pad 0: Focused Clip Slot - always shows current cursor position with full clip functionality
 * - Pad 1: Smart Recording Assistant - context-aware actions (record/undo/advance/create scene)
 * - Pads 2-3: USER mode functionality (freed up from clip slots!)
 * - Pad 4: BWS Track Cycle (same as PERF mode)
 * - Pad 5: Track controls (same as PERF mode) 
 * - Pads 6-9: USER mode functionality (same as PERF mode)
 * 
 * Smart Recording Workflow:
 * 1. Empty slot + Pad1 → Start recording (same as Pad0)
 * 2. Recording + Pad1 → Immediate undo (brilliant for "oops" moments)  
 * 3. Playing + Pad1 → Auto-advance to next free slot + start recording
 * 4. No free slots + Pad1 → Auto-create scene + select + start recording
 */
public class Perf2ConsolePrinter extends BaseConsolePrinter implements HasControllsForPage, BwsTrackDiscoveryService.LedUpdateCallback {
    
    // Pad assignments for PERF2 mode
    private static final int FOCUSED_CLIP_PAD = 0;        // Pad0: Current cursor clip slot
    private static final int SMART_ASSISTANT_PAD = 1;     // Pad1: Smart recording assistant  
    private static final int TRACK_CYCLE_PAD = 4;         // Pad4: BWS track cycling (same as PERF)
    
    private final Page page;
    private final ApiManager apiManager;
    private final PadConfigurationManager padConfigManager;
    
    // Reuse existing functionality for hybrid pads
    private final UserControlls userControls;             // For pads 2-3, 6-9
    private final ClipControls clipControls;              // For pad 5 (track controls)
    
    // PERF2-specific functionality
    private final CursorTrack cursorTrack;
    private final ClipLauncherSlotBank clipSlotBank;  // 4-slot window (shared with other modes)
    private ClipLauncherSlot focusedClipSlot;         // Current cursor position
    private final SceneBank sceneBank;
    
    // State tracking for smart assistant  
    private int currentClipSlotIndex = 0;             // Current cursor position in track (bank-relative)
    private ClipLauncherSlot lastKnownFocusedSlot;    // Backup reference to focused slot
    
    public Perf2ConsolePrinter(Page page, ApiManager apiManager, PadConfigurationManager padConfigManager) {
        super(apiManager.getHost());
        this.page = page;
        this.apiManager = apiManager;
        this.padConfigManager = padConfigManager;
        
        // Get Bitwig API objects for clip slot management
        this.cursorTrack = apiManager.getTrackCurser();
        // PERF2 mode gets the larger clip slot bank for full track access (128+ slots)
        this.clipSlotBank = apiManager.getPerf2SlotBank();
        this.sceneBank = apiManager.getSceneBank();
        
        // Initialize focused clip slot (first slot by default)
        this.focusedClipSlot = clipSlotBank.getItemAt(currentClipSlotIndex);
        this.lastKnownFocusedSlot = this.focusedClipSlot;
        
        // Debug: Log the actual bank size we got
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Initialized with clip slot bank size: %d", clipSlotBank.getSizeOfBank()));
        
        // Create instances of existing functionality for hybrid mode
        this.userControls = new UserControlls(Page.USER, apiManager, padConfigManager);
        this.clipControls = new ClipControls(Page.CLIP, apiManager);
        
        // Set up BWS LED callback for track selection feedback (same as PERF mode)
        BwsTrackDiscoveryService bwsService = apiManager.getBwsTrackDiscoveryService();
        if (bwsService != null) {
            bwsService.setLedUpdateCallback(this);
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, "PERF2: BWS LED callback registered");
        }
        
        // Set up clip slot observation for LED feedback
        setupClipSlotObservers();
        
        // Set initial LED states
        updateFocusedClipLed();
        updateSmartAssistantLed();
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, "PERF2: Smart recording workflow initialized");
    }
    
    @Override
    public Page getPage() {
        return page;
    }
    
    @Override
    public void processControlls(List<Softstep1Pad> pushedDownPads, ShortMidiMessage msg) {
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format("PERF2: Processing %d pads", pushedDownPads.size()));
        
        // Split pads into different functional groups
        List<Softstep1Pad> focusedClipPads = new ArrayList<>();
        List<Softstep1Pad> smartAssistantPads = new ArrayList<>();
        List<Softstep1Pad> userPads = new ArrayList<>();
        List<Softstep1Pad> clipPads = new ArrayList<>();
        List<Softstep1Pad> trackCyclePads = new ArrayList<>();
        
        for (Softstep1Pad pad : pushedDownPads) {
            int padIndex = pad.getNumber();
            
            if (padIndex == FOCUSED_CLIP_PAD) {
                focusedClipPads.add(pad);
            } else if (padIndex == SMART_ASSISTANT_PAD) {
                smartAssistantPads.add(pad);
            } else if (padIndex == TRACK_CYCLE_PAD) {
                trackCyclePads.add(pad);
            } else if (padIndex == 5) {
                // Pad 5: Track controls (mute/arm) - use CLIP functionality
                clipPads.add(pad);
            } else if (isUserPad(padIndex)) {
                // Pads 2-3, 6-9: USER mode functionality
                userPads.add(pad);
            }
        }
        
        // Route to appropriate handlers
        if (!focusedClipPads.isEmpty()) {
            processFocusedClipPads(focusedClipPads, msg);
        }
        
        if (!smartAssistantPads.isEmpty()) {
            processSmartAssistantPads(smartAssistantPads, msg);
        }
        
        if (!userPads.isEmpty()) {
            userControls.processControlls(userPads, msg);
        }
        
        if (!clipPads.isEmpty()) {
            clipControls.processControlls(clipPads, msg);
        }
        
        if (!trackCyclePads.isEmpty()) {
            processTrackCyclePads(trackCyclePads, msg);
        }
    }
    
    /**
     * Processes Pad0 - Focused Clip Slot functionality.
     * This pad always represents the current cursor position and behaves like a standard clip slot.
     */
    private void processFocusedClipPads(List<Softstep1Pad> focusedClipPads, ShortMidiMessage msg) {
        for (Softstep1Pad pad : focusedClipPads) {
            Gestures gestures = pad.gestures();
            
            // Only log when there's actual pad activity (not idle state)
            if (gestures.getPressure() > 0 || gestures.isFootOn() || gestures.isFootOff()) {
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                    "PERF2: Processing Focused Clip PAD%d (pressure: %d, footOn: %s, footOff: %s, longPress: %s)", 
                    pad.getNumber(), gestures.getPressure(), gestures.isFootOn(), gestures.isFootOff(), gestures.isLongPress()));
            }
            
            // Handle long press - delete clip (same as CLIP mode)
            if (gestures.isLongPress()) {
                handleFocusedClipLongPress();
                pad.notifyControlConsumed();
            }
            // Handle normal press - clip slot interaction
            else if (gestures.isFootOn()) {
                handleFocusedClipAction();
                pad.notifyControlConsumed();
            }
        }
    }
    
    /**
     * Handles long press on focused clip slot - deletes the clip (same as CLIP mode).
     */
    private void handleFocusedClipLongPress() {
        if (focusedClipSlot == null) return;
        
        boolean hasContent = focusedClipSlot.hasContent().get();
        
        if (hasContent) {
            focusedClipSlot.deleteObject();
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, "PERF2: Long press - Deleted clip content");
        } else {
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, "PERF2: Long press - No content to delete");
        }
    }
    
    /**
     * Finds the focused clip slot by comparing clip states with our last known focused slot.
     * This method can survive bank scrolling by finding the slot with matching characteristics.
     */
    private void refreshFocusedClipSlot() {
        if (lastKnownFocusedSlot == null) {
            // Fallback to current index
            focusedClipSlot = clipSlotBank.getItemAt(currentClipSlotIndex);
            lastKnownFocusedSlot = focusedClipSlot;
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: No last known slot, using current index: " + currentClipSlotIndex);
            return;
        }
        
        // Save the last known slot's characteristics
        boolean lastHasContent = lastKnownFocusedSlot.hasContent().get();
        boolean lastIsPlaying = lastKnownFocusedSlot.isPlaying().get();
        boolean lastIsRecording = lastKnownFocusedSlot.isRecording().get();
        
        // Search the current bank view for a slot with matching characteristics
        boolean foundMatch = false;
        for (int i = 0; i < clipSlotBank.getSizeOfBank(); i++) {
            ClipLauncherSlot candidate = clipSlotBank.getItemAt(i);
            if (candidate != null) {
                boolean candHasContent = candidate.hasContent().get();
                boolean candIsPlaying = candidate.isPlaying().get();
                boolean candIsRecording = candidate.isRecording().get();
                
                if (candHasContent == lastHasContent && candIsPlaying == lastIsPlaying && candIsRecording == lastIsRecording) {
                    // Found a match - this is likely our original slot
                    currentClipSlotIndex = i;
                    focusedClipSlot = candidate;
                    lastKnownFocusedSlot = candidate;
                    foundMatch = true;
                    
                    DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                        "PERF2: Found matching focused clip slot at bank index: %d (content:%s, playing:%s, recording:%s)", 
                        i, candHasContent, candIsPlaying, candIsRecording));
                    break;
                }
            }
        }
        
        if (!foundMatch) {
            // Fallback - use current index and update our tracking
            focusedClipSlot = clipSlotBank.getItemAt(currentClipSlotIndex);
            lastKnownFocusedSlot = focusedClipSlot;
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                "PERF2: No matching slot found, fallback to current index: %d", currentClipSlotIndex));
        }
    }
    
    /**
     * Handles the focused clip slot action based on current state.
     */
    private void handleFocusedClipAction() {
        // Refresh focused clip slot reference (no scrolling needed with full track access)
        refreshFocusedClipSlot();
        
        if (focusedClipSlot == null) return;
        
        boolean hasContent = focusedClipSlot.hasContent().get();
        boolean isPlaying = focusedClipSlot.isPlaying().get();
        boolean isRecording = focusedClipSlot.isRecording().get();
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Focused clip action - hasContent: %s, isPlaying: %s, isRecording: %s", 
            hasContent, isPlaying, isRecording));
        
        if (!hasContent) {
            // Empty slot - start recording
            focusedClipSlot.launch();
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, "PERF2: Started recording in focused slot");
        } else if (isRecording) {
            // Recording - stop and play
            focusedClipSlot.launch();
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, "PERF2: Stopped recording, started playback");
        } else if (isPlaying) {
            // Playing - stop using track stop
            cursorTrack.stop();
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, "PERF2: Stopped playback");
        } else {
            // Has content but not playing - start playing
            focusedClipSlot.launch();
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, "PERF2: Started playback of existing clip");
        }
    }
    
    /**
     * Processes Pad1 - Smart Recording Assistant functionality.
     * This is the heart of PERF2 mode - context-aware intelligent assistance.
     */
    private void processSmartAssistantPads(List<Softstep1Pad> smartAssistantPads, ShortMidiMessage msg) {
        for (Softstep1Pad pad : smartAssistantPads) {
            Gestures gestures = pad.gestures();
            
            // Only log when there's actual pad activity
            if (gestures.getPressure() > 0 || gestures.isFootOn() || gestures.isFootOff()) {
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                    "PERF2: Processing Smart Assistant PAD%d (pressure: %d, footOn: %s, footOff: %s)", 
                    pad.getNumber(), gestures.getPressure(), gestures.isFootOn(), gestures.isFootOff()));
            }
            
            // Handle smart assistant action on foot press
            if (gestures.isFootOn()) {
                handleSmartAssistantAction();
                pad.notifyControlConsumed();
            }
        }
    }
    
    /**
     * Handles smart assistant action based on current focused clip state.
     * This is the core intelligence of PERF2 mode.
     */
    private void handleSmartAssistantAction() {
        if (focusedClipSlot == null) return;
        
        boolean hasContent = focusedClipSlot.hasContent().get();
        boolean isPlaying = focusedClipSlot.isPlaying().get();
        boolean isRecording = focusedClipSlot.isRecording().get();
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Smart assistant context - hasContent: %s, isPlaying: %s, isRecording: %s", 
            hasContent, isPlaying, isRecording));
        
        if (!hasContent) {
            // State 1: Empty slot - start recording (same as Pad0)
            focusedClipSlot.launch();
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, "PERF2: Smart Assistant - Started recording (empty slot)");
            
        } else if (isRecording) {
            // State 2: Recording - undo recording (delete clip content)
            focusedClipSlot.deleteObject();
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, "PERF2: Smart Assistant - Undid recording");
            
        } else if (isPlaying) {
            // State 3: Playing - auto-advance to next free slot
            advanceToNextFreeSlotAndRecord();
            
        } else {
            // Clip has content but not playing - treat as playing state for auto-advance
            advanceToNextFreeSlotAndRecord();
        }
        
        // Update LED states after action
        updateFocusedClipLed();
        updateSmartAssistantLed();
    }
    
    /**
     * Advances to the next free clip slot and starts recording.
     * Uses simple 4-slot cycle recording - when bank is full, clear all and restart from slot 0.
     */
    private void advanceToNextFreeSlotAndRecord() {
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Advance requested - current clip slot index: %d", currentClipSlotIndex));
        
        // Search for next free slot in 4-slot bank (simple cycle)
        int nextFreeSlotIndex = findNextFreeSlotIndex();
        
        if (nextFreeSlotIndex != -1) {
            // Found free slot - switch focus and start recording
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                "PERF2: Found free slot at index %d, switching from index %d", 
                nextFreeSlotIndex, currentClipSlotIndex));
                
            // Update focus to the free slot
            currentClipSlotIndex = nextFreeSlotIndex;
            focusedClipSlot = clipSlotBank.getItemAt(currentClipSlotIndex);
            lastKnownFocusedSlot = focusedClipSlot;
            
            // Start recording in the new slot
            focusedClipSlot.launch();
            
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                "PERF2: Smart Assistant - Advanced to index %d and started recording", nextFreeSlotIndex));
        } else {
            // No free slots found in 4-slot bank - start new cycle
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Bank full - clearing all clips and starting new cycle");
                
            // Clear all clips in current bank (reuse CLIP mode functionality)
            apiManager.getApiToHost().deleteAllSlots();
            
            // Reset to slot 0 and start recording
            currentClipSlotIndex = 0;
            focusedClipSlot = clipSlotBank.getItemAt(currentClipSlotIndex);
            lastKnownFocusedSlot = focusedClipSlot;
            
            // Start recording in slot 0
            focusedClipSlot.launch();
            
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Cycle restart - cleared bank and started recording in slot 0");
        }
    }
    
    /**
     * Finds the next free clip slot in the 4-slot bank.
     * Simple cycle: 0→1→2→3→clear→0→1→2→3...
     * @return Index of next free slot (0-3), or -1 if bank is full
     */
    private int findNextFreeSlotIndex() {
        int bankSize = clipSlotBank.getSizeOfBank(); // Should be 4
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Searching for free slot from current index %d in bank size %d", currentClipSlotIndex, bankSize));
        
        // Search from current position forward through the 4-slot bank
        for (int i = currentClipSlotIndex + 1; i < bankSize; i++) {
            ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            boolean hasContent = slot.hasContent().get();
            
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                "PERF2: Checking slot %d/%d - hasContent: %s", i, bankSize, hasContent));
                
            if (!hasContent) {
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                    "PERF2: Found free slot at index %d in 4-slot cycle", i));
                return i;
            }
        }
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
            "PERF2: No free slots found in 4-slot bank - cycle restart needed");
        return -1;
    }
    
    /**
     * Checks if there are any free slots available for LED display purposes.
     * This method NEVER scrolls to avoid interfering with the focused clip reference.
     * @return true if at least one free slot is available in the current bank view
     */
    private boolean checkForNextFreeSlotForLED() {
        int bankSize = clipSlotBank.getSizeOfBank();
        
        // Only check within current bank view - NEVER scroll for LED updates!
        for (int i = currentClipSlotIndex + 1; i < bankSize; i++) {
            ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            if (!slot.hasContent().get()) {
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                    "PERF2: LED check - Found free slot at bank index %d (no scrolling)", i));
                return true;
            }
        }
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
            "PERF2: LED check - No free slots in current bank view");
        return false;
    }
    
    /**
     * Processes BWS Track Cycle pads (same as PERF mode).
     */
    private void processTrackCyclePads(List<Softstep1Pad> trackCyclePads, ShortMidiMessage msg) {
        for (Softstep1Pad pad : trackCyclePads) {
            int padIndex = pad.getNumber();
            Gestures gestures = pad.gestures();
            
            // Only log when there's actual pad activity
            if (gestures.getPressure() > 0 || gestures.isFootOn() || gestures.isFootOff()) {
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                    "PERF2: Processing BWS Track Cycle PAD%d (pressure: %d, footOn: %s, footOff: %s)", 
                    padIndex, gestures.getPressure(), gestures.isFootOn(), gestures.isFootOff()));
            }
            
            if (gestures.isFootOn()) {
                BwsTrackDiscoveryService bwsService = apiManager.getBwsTrackDiscoveryService();
                
                if (bwsService != null && bwsService.isInitialized()) {
                    boolean cycled = bwsService.cycleToNextBwsTrack();
                    
                    if (cycled) {
                        int currentSlot = bwsService.getCurrentBwsSlot();
                        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                            "PERF2: BWS track cycle SUCCESS - current slot: %d", currentSlot));
                    } else {
                        DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                            "PERF2: No BWS tracks available for cycling");
                    }
                }
                
                pad.notifyControlConsumed();
            }
        }
    }
    
    /**
     * Sets up observers for focused clip slot changes to update LED feedback.
     */
    private void setupClipSlotObservers() {
        // Observe all clip slots for content and state changes
        for (int i = 0; i < clipSlotBank.getSizeOfBank(); i++) {
            ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            
            slot.hasContent().markInterested();
            slot.isPlaying().markInterested();
            slot.isRecording().markInterested();
            
            // Add observers to update LEDs when slot states change
            slot.hasContent().addValueObserver(hasContent -> {
                if (slot == focusedClipSlot) {
                    updateFocusedClipLed();
                    updateSmartAssistantLed();
                }
            });
            
            slot.isPlaying().addValueObserver(isPlaying -> {
                if (slot == focusedClipSlot) {
                    updateFocusedClipLed();
                    updateSmartAssistantLed();
                }
            });
            
            slot.isRecording().addValueObserver(isRecording -> {
                if (slot == focusedClipSlot) {
                    updateFocusedClipLed();
                    updateSmartAssistantLed();
                }
            });
        }
    }
    
    /**
     * Updates LED feedback for focused clip slot (Pad0) based on current state.
     */
    private void updateFocusedClipLed() {
        if (focusedClipSlot == null) return;
        
        boolean hasContent = focusedClipSlot.hasContent().get();
        boolean isPlaying = focusedClipSlot.isPlaying().get();
        boolean isRecording = focusedClipSlot.isRecording().get();
        
        LedStates ledState;
        
        if (!hasContent) {
            ledState = Page.PERF2_LED_STATES.FOCUSED_EMPTY;          // OFF
        } else if (isRecording) {
            ledState = Page.PERF2_LED_STATES.FOCUSED_RECORDING;      // RED + BLINK
        } else if (isPlaying) {
            ledState = Page.PERF2_LED_STATES.FOCUSED_PLAYING;        // YELLOW
        } else {
            ledState = Page.PERF2_LED_STATES.FOCUSED_STOPPED;        // GREEN
        }
        
        apiManager.getSoftstepController().updateLedStatesForPerfMode(Page.PERF2, FOCUSED_CLIP_PAD, ledState);
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Updated Focused Clip LED - hasContent: %s, isPlaying: %s, isRecording: %s → %s", 
            hasContent, isPlaying, isRecording, ledState.toString()));
    }
    
    /**
     * Updates LED feedback for smart assistant (Pad1) based on focused clip context.
     */
    private void updateSmartAssistantLed() {
        if (focusedClipSlot == null) return;
        
        boolean hasContent = focusedClipSlot.hasContent().get();
        boolean isPlaying = focusedClipSlot.isPlaying().get();
        boolean isRecording = focusedClipSlot.isRecording().get();
        
        LedStates ledState;
        
        if (!hasContent) {
            // State 1: Ready to start recording
            ledState = Page.PERF2_LED_STATES.SMART_READY_TO_RECORD;       // GREEN
        } else if (isRecording) {
            // State 2: Undo available
            ledState = Page.PERF2_LED_STATES.SMART_UNDO_AVAILABLE;        // RED + BLINK
        } else if (isPlaying || hasContent) {
            // State 3: Auto-advance ready (playing or has content)
            boolean hasNextFreeSlot = checkForNextFreeSlotForLED();
            if (hasNextFreeSlot) {
                ledState = Page.PERF2_LED_STATES.SMART_AUTO_ADVANCE;      // GREEN + BLINK
            } else {
                // State 4: Cycle restart mode (no free slots - will clear all and restart)
                ledState = Page.PERF2_LED_STATES.SMART_SCENE_CREATION;    // GREEN + FAST_BLINK
            }
        } else {
            // Fallback
            ledState = Page.PERF2_LED_STATES.SMART_READY_TO_RECORD;       // GREEN
        }
        
        apiManager.getSoftstepController().updateLedStatesForPerfMode(Page.PERF2, SMART_ASSISTANT_PAD, ledState);
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Updated Smart Assistant LED - context: hasContent:%s, isPlaying:%s, isRecording:%s → %s", 
            hasContent, isPlaying, isRecording, ledState.toString()));
    }
    
    /**
     * Determines if a pad should use USER mode functionality in PERF2 mode.
     * USER pads in PERF2: 2, 3, 6, 7, 8, 9
     */
    private boolean isUserPad(int padIndex) {
        return (padIndex == 2 || padIndex == 3) || (padIndex >= 6 && padIndex <= 9);
    }
    
    /**
     * BWS LED callback implementation (same as PERF mode).
     */
    @Override
    public void updateBwsLed(int bwsSlot) {
        LedStates ledState;
        
        switch (bwsSlot) {
            case 0: ledState = Page.USER_LED_STATES.BWS_TRACK_0; break;
            case 1: ledState = Page.USER_LED_STATES.BWS_TRACK_1; break;
            case 2: ledState = Page.USER_LED_STATES.BWS_TRACK_2; break;
            case 3: ledState = Page.USER_LED_STATES.BWS_TRACK_3; break;
            case 4: ledState = Page.USER_LED_STATES.BWS_TRACK_4; break;
            case 5: ledState = Page.USER_LED_STATES.BWS_TRACK_5; break;
            case -2: ledState = Page.USER_LED_STATES.BWS_NON_BWS_TRACK; break;
            default: ledState = Page.USER_LED_STATES.BWS_INACTIVE; break;
        }
        
        apiManager.getSoftstepController().updateLedStatesForPerfMode(Page.PERF2, TRACK_CYCLE_PAD, ledState);
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: BWS LED Callback - Updated PAD%d to show BWS:%d state (%s)", 
            TRACK_CYCLE_PAD, bwsSlot, ledState.toString()));
    }
}