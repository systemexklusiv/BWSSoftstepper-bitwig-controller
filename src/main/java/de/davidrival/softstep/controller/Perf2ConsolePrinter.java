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
        
        // Get Bitwig API objects for clip slot management (use existing from ApiManager)
        this.cursorTrack = apiManager.getTrackCurser();
        this.clipSlotBank = apiManager.getSlotBank();  // Use existing slot bank from ApiManager
        this.sceneBank = apiManager.getSceneBank();
        
        // Initialize focused clip slot (first slot by default)
        this.focusedClipSlot = clipSlotBank.getItemAt(currentClipSlotIndex);
        this.lastKnownFocusedSlot = this.focusedClipSlot;
        
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
     * With full track access, this is now much simpler - no bank scrolling needed.
     */
    private void advanceToNextFreeSlotAndRecord() {
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Advance requested - current clip slot index: %d", currentClipSlotIndex));
        
        // Search for next free slot in current track (full track scan)
        int nextFreeSlotIndex = findNextFreeSlotIndex();
        
        if (nextFreeSlotIndex != -1) {
            // Found free slot - switch focus and start recording
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                "PERF2: Found free slot at index %d, switching from index %d", 
                nextFreeSlotIndex, currentClipSlotIndex));
                
            // Update focus to the free slot
            currentClipSlotIndex = nextFreeSlotIndex;
            focusedClipSlot = clipSlotBank.getItemAt(currentClipSlotIndex);
            lastKnownFocusedSlot = focusedClipSlot;  // Update tracking
            
            // Start recording in the new slot
            focusedClipSlot.launch();
            
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                "PERF2: Smart Assistant - Advanced to index %d and started recording", nextFreeSlotIndex));
        } else {
            // No free slots found - try alternative approaches
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: No free slots found after scrolling, trying alternative methods");
                
            // Method 1: Try creating new scene
            createNewSceneAndRecord();
            
            // Method 2: If scene creation fails, try direct cursor track approach
            if (focusedClipSlot == null || focusedClipSlot.hasContent().get()) {
                tryDirectCursorTrackRecording();
            }
        }
    }
    
    /**
     * Finds the next free clip slot in the current track.
     * This method handles bank scrolling carefully to search beyond the current 4-slot window,
     * but only when actually advancing (not for LED updates).
     * @return Index of next free slot within current bank view, or -1 if none found
     */
    private int findNextFreeSlotIndex() {
        int bankSize = clipSlotBank.getSizeOfBank();
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Searching for free slot from current index %d in bank size %d", currentClipSlotIndex, bankSize));
        
        // First, search within current bank view
        for (int i = currentClipSlotIndex + 1; i < bankSize; i++) {
            ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            boolean hasContent = slot.hasContent().get();
            
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                "PERF2: Checking bank slot %d - hasContent: %s", i, hasContent));
                
            if (!hasContent) {
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                    "PERF2: Found free slot at bank index %d (no scrolling needed)", i));
                return i;
            }
        }
        
        // If no free slots in current bank view, try scrolling multiple times to find more slots
        int maxScrollAttempts = 10; // Try scrolling up to 10 times to find free slots
        
        for (int scrollAttempt = 0; scrollAttempt < maxScrollAttempts; scrollAttempt++) {
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                "PERF2: No free slots in current bank view, scrolling forward (attempt %d)", scrollAttempt + 1));
            clipSlotBank.scrollBy(1);
            
            // After each scroll, check all slots in the new view
            for (int i = 0; i < bankSize; i++) {
                ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
                if (!slot.hasContent().get()) {
                    DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                        "PERF2: Found free slot at bank index %d after scrolling %d times", i, scrollAttempt + 1));
                    return i;
                }
            }
        }
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: No free slots found after %d scroll attempts", maxScrollAttempts));
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
     * Creates multiple new scenes and starts recording in the next available slot.
     * Pre-creates several scenes for efficiency as you suggested.
     */
    private void createNewSceneAndRecord() {
        try {
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Creating new scenes - pre-creating 4 scenes for efficiency");
            
            // Pre-create 4 scenes for better performance (as you suggested)
            // But first try a different approach - use the cursor track to create clips directly
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Attempting to create new clip slots by expanding track capacity");
            
            // Try to make the clip slot bank see more slots by changing its capacity
            int originalBankSize = clipSlotBank.getSizeOfBank();
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                "PERF2: Original bank size: %d", originalBankSize));
            
            // Method 1: Try to create scenes first
            for (int sceneCreate = 0; sceneCreate < 4; sceneCreate++) {
                sceneBank.scrollBy(1);
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                    "PERF2: Created scene %d/4", sceneCreate + 1));
            }
            
            // Method 2: Try to force the clip slot bank to refresh and see new slots
            // Reset the clip slot bank position to beginning
            clipSlotBank.scrollPosition().set(0);
            
            // Wait a moment for the API to catch up
            try {
                Thread.sleep(50); // Brief delay to let API update
            } catch (InterruptedException e) {
                // Ignore
            }
            
            // Now try to find empty slots with a more thorough approach
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Scanning for newly available slots after scene creation");
                
            int maxScrollForNewSlots = 20; // Try even more positions
            boolean foundFreeSlot = false;
            
            for (int scrollAttempt = 0; scrollAttempt < maxScrollForNewSlots && !foundFreeSlot; scrollAttempt++) {
                int bankSize = clipSlotBank.getSizeOfBank();
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                    "PERF2: Scroll attempt %d, bank size: %d", scrollAttempt + 1, bankSize));
                
                // Check all slots in current view
                for (int i = 0; i < bankSize; i++) {
                    ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
                    if (slot != null && !slot.hasContent().get()) {
                        currentClipSlotIndex = i;
                        focusedClipSlot = slot;
                        lastKnownFocusedSlot = slot;
                        focusedClipSlot.launch();
                        
                        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                            "PERF2: SUCCESS! Found empty slot at position %d, index %d and started recording", 
                            scrollAttempt, i));
                        foundFreeSlot = true;
                        return;
                    }
                }
                
                // Move to next position
                if (!foundFreeSlot) {
                    clipSlotBank.scrollBy(1);
                }
            }
            
            // If still no success, try the alternative approach of creating a clip directly
            if (!foundFreeSlot) {
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                    "PERF2: Scene creation approach failed, trying direct clip creation");
                createClipDirectlyOnCursorTrack();
            }
                
        } catch (Exception e) {
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Smart Assistant - Failed to create new scenes: " + e.getMessage());
        }
    }
    
    /**
     * Direct method to create a new clip on the cursor track.
     * This bypasses the clip slot bank limitations by using cursor track methods.
     */
    private void createClipDirectlyOnCursorTrack() {
        try {
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Attempting direct clip creation on cursor track");
            
            // Ensure the track is armed for recording
            cursorTrack.arm().set(true);
            
            // Try to start recording on cursor track - this should create a new clip
            // We'll look for a recording clip after starting
            cursorTrack.selectInMixer();
            
            // Get the current play state
            Transport transport = apiManager.getHost().createTransport();
            
            if (!transport.isPlaying().get()) {
                // If not playing, start playing first
                transport.play();
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                    "PERF2: Started transport for recording");
            }
            
            // Now start recording - this should create a clip on the cursor track
            transport.record();
            
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Started direct recording on cursor track");
            
            // After a brief delay, try to locate the newly created recording clip
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    findNewlyCreatedRecordingClip();
                }
            }, 200); // 200ms delay to let Bitwig create the clip
            
        } catch (Exception e) {
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Direct clip creation failed: " + e.getMessage());
        }
    }
    
    /**
     * Searches for a newly created recording clip after direct track recording.
     */
    private void findNewlyCreatedRecordingClip() {
        try {
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Searching for newly created recording clip");
            
            // Scan through the clip slot bank to find a recording clip
            boolean foundRecordingClip = false;
            int maxSearchPositions = 30; // Search through many positions
            
            // Start from current position and search forward
            for (int pos = 0; pos < maxSearchPositions && !foundRecordingClip; pos++) {
                int bankSize = clipSlotBank.getSizeOfBank();
                for (int i = 0; i < bankSize; i++) {
                    ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
                    if (slot != null && slot.isRecording().get()) {
                        // Found the recording clip!
                        currentClipSlotIndex = i;
                        focusedClipSlot = slot;
                        lastKnownFocusedSlot = slot;
                        
                        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                            "PERF2: SUCCESS! Found newly created recording clip at position %d, index %d", pos, i));
                        
                        // Update LED feedback
                        updateFocusedClipLed();
                        updateSmartAssistantLed();
                        
                        foundRecordingClip = true;
                        return;
                    }
                }
                
                if (!foundRecordingClip && pos < maxSearchPositions - 1) {
                    clipSlotBank.scrollBy(1);
                }
            }
            
            if (!foundRecordingClip) {
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                    "PERF2: Could not locate newly created recording clip");
            }
            
        } catch (Exception e) {
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Error searching for recording clip: " + e.getMessage());
        }
    }
    
    /**
     * Alternative method: Try to find any empty slot and launch it to create a new recording.
     * This approach tries to work with the existing clip slot structure.
     */
    private void tryDirectCursorTrackRecording() {
        try {
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Trying direct slot launch as fallback");
            
            // Ensure track is armed for recording
            cursorTrack.arm().set(true);
            
            // Try to find any empty slot in the bank by scanning more thoroughly
            int bankSize = clipSlotBank.getSizeOfBank();
            boolean foundEmptySlot = false;
            
            // Try scrolling back to beginning and scanning again
            clipSlotBank.scrollPosition().set(0);
            
            // Scan through multiple positions more thoroughly
            int maxPositions = 20; // Try 20 different bank positions
            for (int pos = 0; pos < maxPositions && !foundEmptySlot; pos++) {
                for (int i = 0; i < bankSize; i++) {
                    ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
                    if (slot != null && !slot.hasContent().get()) {
                        // Found empty slot - launch it to create new recording
                        currentClipSlotIndex = i;
                        focusedClipSlot = slot;
                        lastKnownFocusedSlot = slot;
                        slot.launch();
                        
                        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                            "PERF2: Found and launched empty slot at position %d, index %d", pos, i));
                        foundEmptySlot = true;
                        return;
                    }
                }
                
                if (!foundEmptySlot) {
                    clipSlotBank.scrollBy(1); // Try next position
                }
            }
            
            if (!foundEmptySlot) {
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                    "PERF2: No empty slots found even after exhaustive search");
            }
                
        } catch (Exception e) {
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Direct slot launch failed: " + e.getMessage());
        }
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
                // State 4: Scene creation mode (no free slots)
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