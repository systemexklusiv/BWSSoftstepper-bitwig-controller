package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.BaseConsolePrinter;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance Page - A hybrid mode that combines CLIP and USER functionality.
 * 
 * Layout:
 * - Pads 0-3: CLIP mode functionality (clip slots)
 * - Pad 4: USER mode functionality (configurable UserControl) 
 * - Pad 5: CLIP mode functionality (track control - mute toggle/clip stop)
 * - Pads 6-9: USER mode functionality (configurable UserControls)
 * 
 * This allows musicians to trigger/record clips while using freely assignable UserControls.
 */
public class PerfConsolePrinter extends BaseConsolePrinter implements HasControllsForPage {
    
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
        
        apiManager.getHost().println("PerfPage: Initialized hybrid CLIP+USER performance mode");
    }
    
    @Override
    public Page getPage() {
        return page;
    }
    
    @Override
    public void processControlls(List<Softstep1Pad> pushedDownPads, ShortMidiMessage msg) {
        apiManager.getHost().println(String.format("PerfPage: Processing %d pads", pushedDownPads.size()));
        
        // Split pads into CLIP and USER groups
        List<Softstep1Pad> clipPads = new ArrayList<>();
        List<Softstep1Pad> userPads = new ArrayList<>();
        
        for (Softstep1Pad pad : pushedDownPads) {
            if (isClipPad(pad.getNumber())) {
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
    }
    
    /**
     * Determines if a pad should use CLIP mode functionality.
     * CLIP pads: 0, 1, 2, 3, 5
     * USER pads: 4, 6, 7, 8, 9
     * 
     * @param padIndex The pad index (0-9)
     * @return true if this pad should use CLIP functionality
     */
    private boolean isClipPad(int padIndex) {
        return padIndex <= 3 || padIndex == 5;
    }
    
}