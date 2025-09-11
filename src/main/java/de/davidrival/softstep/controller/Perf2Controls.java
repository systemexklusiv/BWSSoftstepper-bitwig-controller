package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.SceneBank;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.debug.DebugLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance 2 Page - An advanced hybrid mode with focused clip slot and smart recording assistant.
 * 
 * Layout:
 * - Pad 0: Focused Clip Slot (current cursor position - play/record/delete)
 * - Pad 1: Smart Recording Assistant (context-aware recording helper)
 * - Pads 2-3: USER mode functionality (configurable UserControls)
 * - Pad 4: BWS Track Cycle (inherited from BasePerformanceModeControls)
 * - Pad 5: Mute/Stop Clip (inherited from BasePerformanceModeControls) 
 * - Pad 6: Record Arm/Delete All Clips (inherited from BasePerformanceModeControls)
 * - Pads 7-9: USER mode functionality (inherited from BasePerformanceModeControls)
 * 
 * This provides focused clip operation, intelligent recording assistance, and extensive UserControl access.
 */
public class Perf2Controls extends BasePerformanceModeControls {
    
    // Mode-specific pad assignments for PERF2 (lower row)
    private static final int FOCUSED_CLIP_PAD = 0;        // Focused clip slot at cursor position
    private static final int SMART_ASSISTANT_PAD = 1;     // Smart recording assistant
    // Pads 2-3: USER mode (handled by base class isUserPad method)
    
    // PERF2-specific functionality
    private final CursorTrack cursorTrack;
    private final ClipLauncherSlotBank clipSlotBank;
    private ClipLauncherSlot focusedClipSlot;
    private final SceneBank sceneBank;
    
    // State tracking for smart assistant  
    private int currentClipSlotIndex = 0;
    private ClipLauncherSlot lastKnownFocusedSlot;
    
    public Perf2Controls(Page page, ApiManager apiManager, PadConfigurationManager padConfigManager) {
        super(page, apiManager, padConfigManager);
        
        // Get Bitwig API objects for clip slot management
        this.cursorTrack = apiManager.getTrackCurser();
        // PERF2 mode gets the larger clip slot bank for full track access
        this.clipSlotBank = apiManager.getPerf2SlotBank();
        this.sceneBank = apiManager.getSceneBank();
        
        // Initialize focused clip slot (first slot by default)
        this.focusedClipSlot = clipSlotBank.getItemAt(currentClipSlotIndex);
        this.lastKnownFocusedSlot = this.focusedClipSlot;
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Initialized with clip slot bank size: %d", clipSlotBank.getSizeOfBank()));
        
        // Set up PERF2-specific observers
        setupClipSlotObservers();
        setupTrackArmObserver();
        
        DebugLogger.common(apiManager.getHost(), padConfigManager, 
            "PERF2: Initialized focused clip + smart recording mode");
    }
    
    /**
     * Handle mode-specific pads for PERF2 mode (Pads 0-3).
     * Pad 0: Focused clip slot, Pad 1: Smart assistant, Pads 2-3: User controls
     */
    @Override
    protected void processModeSpecificPads(List<Softstep1Pad> modeSpecificPads, ShortMidiMessage msg) {
        // Split mode-specific pads into functional groups
        List<Softstep1Pad> focusedClipPads = new ArrayList<>();
        List<Softstep1Pad> smartAssistantPads = new ArrayList<>();
        List<Softstep1Pad> lowerUserPads = new ArrayList<>();  // Pads 2-3
        
        for (Softstep1Pad pad : modeSpecificPads) {
            int padIndex = pad.getNumber();
            
            if (padIndex == FOCUSED_CLIP_PAD) {
                focusedClipPads.add(pad);
            } else if (padIndex == SMART_ASSISTANT_PAD) {
                smartAssistantPads.add(pad);
            } else if (padIndex == 2 || padIndex == 3) {
                // Pads 2-3 are USER controls in PERF2 mode
                lowerUserPads.add(pad);
            }
        }
        
        // Route to appropriate handlers
        if (!focusedClipPads.isEmpty()) {
            processFocusedClipPads(focusedClipPads, msg);
        }
        
        if (!smartAssistantPads.isEmpty()) {
            processSmartAssistantPads(smartAssistantPads, msg);
        }
        
        if (!lowerUserPads.isEmpty()) {
            // Route pads 2-3 to USER functionality
            userControls.processControlls(lowerUserPads, msg);
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                String.format("PERF2: Processed %d lower user pads (2-3)", lowerUserPads.size()));
        }
    }
    
    /**
     * Override isUserPad to include pads 2-3 in addition to the inherited 7-9.
     * PERF2 USER pads: 2, 3, 7, 8, 9
     */
    @Override
    protected boolean isUserPad(int padIndex) {
        // Include pads 2-3 (lower row) in addition to 7-9 (upper row)
        return (padIndex == 2 || padIndex == 3) || super.isUserPad(padIndex);
    }
    
    /**
     * Refresh mode-specific LED states for PERF2 mode.
     * Updates focused clip and smart assistant LEDs.
     */
    @Override
    protected void refreshModeSpecificLedStates() {
        // Update PERF2-specific LEDs
        updateFocusedClipLed();
        updateSmartAssistantLed();
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
            "PERF2: Mode-specific LED states refreshed");
    }
    
    /**
     * Process focused clip slot pad (Pad 0).
     * Provides direct access to the current cursor position clip slot.
     */
    private void processFocusedClipPads(List<Softstep1Pad> focusedClipPads, ShortMidiMessage msg) {
        for (Softstep1Pad pad : focusedClipPads) {
            if (pad.gestures().isLongPressEvent()) {
                // Long press: Delete focused clip
                if (focusedClipSlot != null && focusedClipSlot.hasContent().get()) {
                    focusedClipSlot.deleteObject();
                    DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                        "PERF2: Deleted focused clip slot content");
                }
                pad.gestures().clearLongPressEvent();
                pad.notifyControlConsumed();
            } else if (pad.gestures().isFootOn()) {
                // Short press: Play/record focused clip
                if (focusedClipSlot != null) {
                    focusedClipSlot.launch();
                    DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                        "PERF2: Launched focused clip slot");
                }
                pad.notifyControlConsumed();
            }
        }
    }
    
    /**
     * Process smart recording assistant pad (Pad 1).
     * Context-aware recording functionality based on current track state.
     */
    private void processSmartAssistantPads(List<Softstep1Pad> smartAssistantPads, ShortMidiMessage msg) {
        for (Softstep1Pad pad : smartAssistantPads) {
            if (pad.gestures().isFootOn()) {
                // Determine action based on current context
                boolean hasContent = focusedClipSlot != null && focusedClipSlot.hasContent().get();
                boolean isRecording = focusedClipSlot != null && focusedClipSlot.isRecording().get();
                boolean isPlaying = focusedClipSlot != null && focusedClipSlot.isPlaying().get();
                
                if (!hasContent) {
                    // State 1: Start recording in empty slot
                    if (focusedClipSlot != null) {
                        focusedClipSlot.launch();
                        DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                            "PERF2: Smart Assistant - Started recording in empty slot");
                    }
                } else if (isRecording) {
                    // State 2: Delete the recording immediately
                    if (focusedClipSlot != null) {
                        focusedClipSlot.deleteObject();
                    }
                    DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                        "PERF2: Smart Assistant - Deleted recording");
                } else if (isPlaying || hasContent) {
                    // State 3: Find next free slot, or delete all if none available
                    findNextFreeSlotOrDeleteAll();
                }
                
                pad.notifyControlConsumed();
            }
        }
    }
    
    /**
     * Find ANY free slot in the bank and record there. If no free slots, delete all and record at slot 0.
     * Smart Assistant looks through the entire bank for any available slot (not just forward).
     */
    private void findNextFreeSlotOrDeleteAll() {
        // First try to find ANY free slot in the entire bank (starting from slot 0)
        boolean foundFreeSlot = false;
        
        for (int i = 0; i < clipSlotBank.getSizeOfBank(); i++) {
            ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            if (!slot.hasContent().get()) {
                // Found free slot, update focused clip and start recording
                setFocusedClipSlot(i);
                apiManager.getApiToHost().fireSlotAt(i);
                foundFreeSlot = true;
                
                DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                    "PERF2: Smart Assistant - Found free slot " + i + ", started recording");
                break;
            }
        }
        
        // If no free slots found, delete all and start at slot 0
        if (!foundFreeSlot) {
            apiManager.getApiToHost().deleteAllSlots();
            setFocusedClipSlot(0);
            apiManager.getApiToHost().fireSlotAt(0);
            
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
                "PERF2: Smart Assistant - No free slots, deleted all and started recording at slot 0");
        }
    }
    
    /**
     * Set up clip slot observation for LED feedback.
     * Sets up observers for ALL clip slots in the bank during initialization.
     * This avoids the "can only be called during driver initialization" error.
     */
    private void setupClipSlotObservers() {
        int bankSize = clipSlotBank.getSizeOfBank();
        
        // Set up observers for all clip slots in the bank
        for (int i = 0; i < bankSize; i++) {
            final int slotIndex = i; // Make effectively final for lambda capture
            ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            
            // Add observers that will update LED when the currently focused slot changes
            slot.hasContent().addValueObserver(hasContent -> {
                if (slotIndex == currentClipSlotIndex) {
                    updateFocusedClipLed();
                }
                // Always update Smart Assistant LED when any slot content changes
                updateSmartAssistantLed();
            });
            
            slot.isPlaying().addValueObserver(isPlaying -> {
                if (slotIndex == currentClipSlotIndex) {
                    updateFocusedClipLed();
                }
            });
            
            slot.isRecording().addValueObserver(isRecording -> {
                if (slotIndex == currentClipSlotIndex) {
                    updateFocusedClipLed();
                }
                // Always update Smart Assistant LED when any slot recording state changes
                updateSmartAssistantLed();
            });
            
            // Mark properties as interested
            slot.hasContent().markInterested();
            slot.isPlaying().markInterested();
            slot.isRecording().markInterested();
        }
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
            "PERF2: Set up observers for all " + bankSize + " clip slots in bank");
    }
    
    /**
     * Updates the focused clip slot reference without touching observers.
     * Simply changes which slot index we're focusing on - observers handle the rest.
     * 
     * @param newSlotIndex The new slot index to focus on
     */
    private void setFocusedClipSlot(int newSlotIndex) {
        currentClipSlotIndex = newSlotIndex;
        focusedClipSlot = clipSlotBank.getItemAt(currentClipSlotIndex);
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, 
            "PERF2: Focused clip slot updated to index " + newSlotIndex);
        
        // Update LED immediately to reflect new focused slot state
        updateFocusedClipLed();
    }
    
    /**
     * Update focused clip slot LED (Pad 0) based on current state.
     */
    private void updateFocusedClipLed(boolean... ignored) {
        updateFocusedClipLed();
    }
    
    private void updateFocusedClipLed() {
        if (focusedClipSlot == null) return;
        
        boolean hasContent = focusedClipSlot.hasContent().get();
        boolean isPlaying = focusedClipSlot.isPlaying().get();
        boolean isRecording = focusedClipSlot.isRecording().get();
        
        LedStates ledState;
        if (!hasContent) {
            ledState = Page.PERF2_LED_STATES.FOCUSED_EMPTY;
        } else if (isRecording) {
            ledState = Page.PERF2_LED_STATES.FOCUSED_RECORDING;
        } else if (isPlaying) {
            ledState = Page.PERF2_LED_STATES.FOCUSED_PLAYING;
        } else {
            ledState = Page.PERF2_LED_STATES.FOCUSED_STOPPED;
        }
        
        // Update LED using base class method
        apiManager.getSoftstepController().updateLedStatesForPerfMode(page, FOCUSED_CLIP_PAD, ledState);
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Updated Focused Clip LED - hasContent: %s, isPlaying: %s, isRecording: %s → %s", 
            hasContent, isPlaying, isRecording, ledState.toString()));
    }
    
    /**
     * Update smart assistant LED (Pad 1) based on current context.
     * LED States:
     * - GREEN BLINK: Free clip slots available and no recording active
     * - YELLOW BLINK: All slots used up (will delete all and start over)  
     * - RED BLINK: Currently recording (can delete recording)
     */
    private void updateSmartAssistantLed() {
        if (focusedClipSlot == null) return;
        
        boolean isRecording = focusedClipSlot.isRecording().get();
        boolean hasAnyFreeSlot = checkForAnyFreeSlot(); // Declare at method scope
        
        LedStates ledState;
        
        if (isRecording) {
            // RED BLINK: Currently recording - can delete the recording
            ledState = Page.PERF2_LED_STATES.SMART_UNDO_AVAILABLE;  // Should be RED + BLINK
        } else {
            if (hasAnyFreeSlot) {
                // GREEN BLINK: Free slots available - can record in next free slot
                ledState = Page.PERF2_LED_STATES.SMART_AUTO_ADVANCE;  // Should be GREEN + BLINK
            } else {
                // YELLOW BLINK: All slots used up - will delete all and start over
                ledState = Page.PERF2_LED_STATES.SMART_SCENE_CREATION;  // Should be YELLOW + BLINK
            }
        }
        
        // Update LED using base class method
        apiManager.getSoftstepController().updateLedStatesForPerfMode(page, SMART_ASSISTANT_PAD, ledState);
        
        DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
            "PERF2: Updated Smart Assistant LED - isRecording:%s, hasAnyFreeSlot:%s → %s", 
            isRecording, hasAnyFreeSlot, ledState.toString()));
    }
    
    /**
     * Check if there are ANY free slots in the entire clip slot bank.
     * Used by Smart Assistant LED to determine GREEN vs YELLOW blink state.
     */
    private boolean checkForAnyFreeSlot() {
        for (int i = 0; i < clipSlotBank.getSizeOfBank(); i++) {
            ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            if (!slot.hasContent().get()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if there are free slots available for auto-advance.
     * (Legacy method - kept for compatibility if needed elsewhere)
     */
    private boolean checkForNextFreeSlot() {
        for (int i = currentClipSlotIndex + 1; i < clipSlotBank.getSizeOfBank(); i++) {
            ClipLauncherSlot slot = clipSlotBank.getItemAt(i);
            if (!slot.hasContent().get()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Set up track arm observation for Pad 6 LED feedback.
     * This ensures Pad 6 shows the correct arm/disarm state when used for CLIP arm functionality.
     */
    private void setupTrackArmObserver() {
        cursorTrack.arm().addValueObserver(armed -> {
            LedStates ledState = armed ? Page.CHANNEL_LED_STATES.ARMED : Page.CHANNEL_LED_STATES.UNARMED;
            
            // Store the LED state in the CLIP page system since Pad 6 uses CLIP functionality
            apiManager.getSoftstepController().updateLedStatesForPerfMode(Page.CLIP, ARM_PAD, ledState);
            
            DebugLogger.perf2(apiManager.getHost(), padConfigManager, String.format(
                "PERF2: Updated Pad 6 Arm LED (via CLIP page) - armed: %s → %s", 
                armed, ledState.toString()));
        });
        
        cursorTrack.arm().markInterested();
    }
}