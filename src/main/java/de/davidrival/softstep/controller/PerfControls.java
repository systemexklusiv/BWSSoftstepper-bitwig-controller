package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.debug.DebugLogger;

import java.util.List;

/**
 * Performance Page - A hybrid mode that combines CLIP and USER functionality with BWS track cycling.
 * 
 * Layout:
 * - Pads 0-3: CLIP mode functionality (clip slots)
 * - Pad 4: BWS Track Cycle (inherited from BasePerformanceModeControls)
 * - Pad 5: Mute/Stop Clip (inherited from BasePerformanceModeControls) 
 * - Pad 6: Record Arm/Delete All Clips (inherited from BasePerformanceModeControls)
 * - Pads 7-9: USER mode functionality (inherited from BasePerformanceModeControls)
 * 
 * This allows musicians to trigger/record clips, navigate BWS tracks, and use freely assignable UserControls.
 */
public class PerfControls extends BasePerformanceModeControls {
    
    public PerfControls(Page page, ApiManager apiManager, PadConfigurationManager padConfigManager) {
        super(page, apiManager, padConfigManager);
        
        DebugLogger.common(apiManager.getHost(), padConfigManager, 
            "PERF: Initialized hybrid CLIP+USER performance mode");
    }
    
    /**
     * Handle mode-specific pads for PERF mode (Pads 0-3).
     * In PERF mode, these are standard CLIP functionality.
     */
    @Override
    protected void processModeSpecificPads(List<Softstep1Pad> modeSpecificPads, ShortMidiMessage msg) {
        // In PERF mode, pads 0-3 use standard CLIP functionality
        if (!modeSpecificPads.isEmpty()) {
            clipControls.processControlls(modeSpecificPads, msg);
            DebugLogger.perf(apiManager.getHost(), padConfigManager, 
                String.format("PERF: Processed %d clip pads (0-3)", modeSpecificPads.size()));
        }
    }
    
    /**
     * Refresh mode-specific LED states for PERF mode.
     * No additional LED refresh needed since clip LEDs are handled by base class.
     */
    @Override
    protected void refreshModeSpecificLedStates() {
        // PERF mode's lower row (pads 0-3) uses standard CLIP LEDs
        // These are already refreshed by the base class clipControls.refreshLedStates()
        DebugLogger.perf(apiManager.getHost(), padConfigManager, 
            "PERF: Mode-specific LED states refreshed");
    }
}