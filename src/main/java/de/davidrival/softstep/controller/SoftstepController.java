package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.BaseConsolePrinter;
import de.davidrival.softstep.debug.DebugLogger;
import de.davidrival.softstep.hardware.SoftstepHardware;

import de.davidrival.softstep.hardware.SoftstepHardwareBase;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Getter
@Setter
public class SoftstepController extends BaseConsolePrinter {

    // Expression pedal disabled to avoid UserControl conflicts with long press (10-19)
    // public static final int USER_CONTROL_INDEX_FOR_PEDAL = 10;
    // public static final int PEDAL_DATA1 = 50;
    // public static final double PEDAL_DATA2_MULTI = 1.95;

    private SoftstepHardware softstepHardware;

    final Controls controls;

    private ApiManager apiManager;

    private ControllerHost controllerHost;

    private ControlerPages pages;
    
    private PadConfigurationManager padConfigManager;

    private List<HasControllsForPage> hasControllsForPages;

    public SoftstepController(
            ControlerPages controlerPages
            , SoftstepHardware softstepHardware
            , ControllerHost hostOrNull
            , PadConfigurationManager padConfigManager) {

        super(hostOrNull);
        this.pages = controlerPages;
        this.softstepHardware = softstepHardware;
        this.padConfigManager = padConfigManager;
        this.apiManager = new ApiManager(hostOrNull, this, padConfigManager);

        this.controls = new Controls(apiManager.getHost());

        hasControllsForPages = new ArrayList<>();
        HasControllsForPage clipControlls = new ClipControls(Page.CLIP, apiManager);
        HasControllsForPage userControlls = new UserControls(Page.USER, apiManager, padConfigManager);
        HasControllsForPage perfPage = new PerfControls(Page.PERF, apiManager, padConfigManager);
        HasControllsForPage perf2Page = new Perf2Controls(Page.PERF2, apiManager, padConfigManager);
        hasControllsForPages.add(clipControlls);
        hasControllsForPages.add(userControlls);
        hasControllsForPages.add(perfPage);
        hasControllsForPages.add(perf2Page);

    }

    public void display() {
        // Get display text - handle PERF2 special case for 4-character limit
        String displayText = pages.getCurrentPage().name();
        if (displayText.equals("PERF2")) {
            displayText = "PRF2";  // Shorten to fit 4-character hardware display
        }
        
        softstepHardware.displayText(displayText);
        softstepHardware.showAllLeds(pages.getCurrentPage());
    }

    public void handleMidi(ShortMidiMessage msg) {
//        p(msg.toString());

        // don't forward midi if consumed for page change
        if (isMidiUsedForPageChange(msg)) return;

        // TODO find out what DATA1 is in native mode for padek
//        if (checkPedal(msg)) return;

        controls.update(msg);
        triggerBitwigIfControlsUsed(controls, msg);
    }

    // Expression pedal method disabled to avoid UserControl conflicts
    /*
    private boolean checkPedal(ShortMidiMessage msg) {
        if (msg.getStatusByte() == 176
        && msg.getData1() == PEDAL_DATA1){
//            p(msg.toString());
//            p(String.valueOf(msg.getData2() * PEDAL_DATA2_MULTI));
            int currentVal = (int) Math.round(msg.getData2() * PEDAL_DATA2_MULTI);
            int pedalVal = currentVal > 127 ? 127 : currentVal;
            apiManager.getApiToHost().setValueOfUserControl(USER_CONTROL_INDEX_FOR_PEDAL, pedalVal);
            return true;
        }
        return false;
    }
    */

    private void triggerBitwigIfControlsUsed(Controls controls, ShortMidiMessage msg) {
        List<Softstep1Pad> pushedDownPads = controls.getPads()
                .stream()
                .filter(Softstep1Pad::isUsed)
                .collect(Collectors.toList());

//        If no controlls where used on the device just exit
        if (pushedDownPads.size() == 0) return;

        hasControllsForPages.stream()
                .filter(c -> c.getPage().equals(pages.getCurrentPage()))
                .findFirst().ifPresent(
                        p -> p.processControlls(pushedDownPads, msg)
                );
    }

    // Navigation pad with 4 directions - any press cycles between pages
    // North: data1=82, East: data1=81, South: data1=83, West: data1=80
    private static final int[] NAV_PAD_ADDRESSES = {80, 81, 82, 83};
    private boolean[] navPadPressed = new boolean[4]; // Track which directions are pressed

    private boolean isMidiUsedForPageChange(ShortMidiMessage msg) {
        if (msg.getStatusByte() == SoftstepHardwareBase.STATUS_BYTE) {
            // Check if this is one of the 4 navigation pad directions
            for (int i = 0; i < NAV_PAD_ADDRESSES.length; i++) {
                if (msg.getData1() == NAV_PAD_ADDRESSES[i]) {
                    boolean wasPressed = navPadPressed[i];
                    boolean isPressed = msg.getData2() > 10; // Threshold for press detection
                    navPadPressed[i] = isPressed;
                    
                    // On rising edge (not pressed -> pressed), cycle pages
                    if (!wasPressed && isPressed) {
                        cyclePage();
                        return true;
                    }
                    return true; // Consume all navigation pad messages
                }
            }
        }
        return false;
    }
    
    private void cyclePage() {
        // Cycle through all four available pages: CLIP → USER → PERF → PERF2 → CLIP
        if (pages.getCurrentPage().equals(Page.CLIP)) {
            pages.setCurrentPage(Page.USER);
        } else if (pages.getCurrentPage().equals(Page.USER)) {
            pages.setCurrentPage(Page.PERF);
        } else if (pages.getCurrentPage().equals(Page.PERF)) {
            pages.setCurrentPage(Page.PERF2);
        } else {
            pages.setCurrentPage(Page.CLIP);
        }
        
        // Refresh LED states for the new mode to ensure they reflect current state
        refreshLedsForCurrentMode();
        
        display();
    }
    
    /**
     * Refreshes LED states when switching to a new mode.
     * This ensures LEDs reflect the current state of clips/controls.
     */
    private void refreshLedsForCurrentMode() {
        Page currentPage = pages.getCurrentPage();
        
        apiManager.getHost().println("SoftstepController: Refreshing LED states for mode: " + currentPage.name());
        
        // Find the controller for the current page and refresh its LED states
        for (HasControllsForPage controller : hasControllsForPages) {
            if (controller.getPage().equals(currentPage)) {
                apiManager.getHost().println("SoftstepController: Found controller for " + currentPage.name() + ", calling refreshLedStates()");
                controller.refreshLedStates();
                break;
            }
        }
    }
    


    /**
     * Write to the states of each page
     * Doesn't matter is currently active or not
     * Needed if one wants to switch to another mode. Afterwards it sends out the
     * changed led states to the control for the acive page
     *
     * @param page      the page where the LED is to be set (not importand if active or not
     * @param index     the index of the controller ( e.g. the slotindex if used in clip mode)
     * @param ledStates the led states, meaninf color and flashin mode
     */
    public void updateLedStates(Page page, int index, LedStates ledStates) {
        // First write to the states of each page
        // Doesn't matter is currently active or not
        // Needed if one wants to switch to another mode
        pages.distributeLedStates(page, index, ledStates);

//        p(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
//        p(Page.CLIP.ledStates.toString());
//        p(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");

        // Only render the led states of the active page
        if (pages.getCurrentPage().equals(page)) {
            softstepHardware.drawLedAt(index, ledStates);
        }
    }
    
    /**
     * Updates LED states for PERF and PERF2 modes, which need special handling since they're hybrid modes.
     * PERF mode uses CLIP functionality for pads 0-3,5 and USER functionality for pads 4,6-9.
     * PERF2 mode uses focused clip (0), smart assistant (1), USER (2-3), and upper row like PERF.
     * This method ensures LEDs are updated correctly regardless of which internal page system
     * is calling the update.
     * 
     * @param page The original page making the LED update call (Page.CLIP, Page.USER, or Page.PERF2)
     * @param index The pad index (0-9)
     * @param ledStates The LED states to apply
     */
    public void updateLedStatesForPerfMode(Page page, int index, LedStates ledStates) {
        // Store LED state in the original page system (for mode switching)
        pages.distributeLedStates(page, index, ledStates);
        
        // If we're currently in PERF mode, check if this pad assignment is valid
        if (pages.getCurrentPage().equals(Page.PERF)) {
            if (shouldRenderLedInPerfMode(page, index)) {
                softstepHardware.drawLedAt(index, ledStates);
                
                // Debug logging
                DebugLogger.perf(getHost(), padConfigManager, String.format("PERF Mode LED Update: Pad %d from %s page with state %s", 
                    index, page.toString(), ledStates.toString()));
            } else {
                // Debug logging for blocked updates
                DebugLogger.perf(getHost(), padConfigManager, String.format("PERF Mode LED BLOCKED: Pad %d from %s page (assigned to different subsystem)", 
                    index, page.toString()));
            }
        }
        // If we're currently in PERF2 mode, check if this pad assignment is valid
        else if (pages.getCurrentPage().equals(Page.PERF2)) {
            if (shouldRenderLedInPerf2Mode(page, index)) {
                softstepHardware.drawLedAt(index, ledStates);
                
                // Debug logging
                DebugLogger.perf2(getHost(), padConfigManager, String.format("PERF2 Mode LED Update: Pad %d from %s page with state %s", 
                    index, page.toString(), ledStates.toString()));
            } else {
                // Debug logging for blocked updates
                DebugLogger.perf2(getHost(), padConfigManager, String.format("PERF2 Mode LED BLOCKED: Pad %d from %s page (assigned to different subsystem)", 
                    index, page.toString()));
            }
        }
        // If not in PERF or PERF2 mode, fall back to normal behavior
        else if (pages.getCurrentPage().equals(page)) {
            softstepHardware.drawLedAt(index, ledStates);
        }
    }
    
    /**
     * Determines if a LED update should be rendered in PERF mode based on pad assignments.
     * PERF mode pad assignment:
     * - Pads 0-3, 5: CLIP mode
     * - Pad 4: TRACK_CYCLE (handled by PERF mode directly)
     * - Pads 6-9: USER mode
     * 
     * @param sourcePage The page/subsystem requesting the LED update
     * @param padIndex The pad index (0-9)
     * @return true if this LED update should be rendered in PERF mode
     */
    private boolean shouldRenderLedInPerfMode(Page sourcePage, int padIndex) {
        switch (sourcePage) {
            case CLIP:
                // CLIP mode controls pads 0-3 and 5 in PERF mode
                return (padIndex <= 3) || (padIndex == 5);
            
            case USER:
                // USER mode controls pads 6-9 in PERF mode
                return (padIndex >= 6 && padIndex <= 9);
            
            case PERF:
                // PERF mode can update any pad (for BWS track cycle on pad 4, etc.)
                return true;
            
            default:
                // Unknown source page - allow for backward compatibility
                return true;
        }
    }
    
    /**
     * Determines if a LED update should be rendered in PERF2 mode based on pad assignments.
     * PERF2 mode pad assignment:
     * - Pad 0: Focused Clip (handled by PERF2 mode directly)
     * - Pad 1: Smart Assistant (handled by PERF2 mode directly) 
     * - Pads 2-3: USER mode
     * - Pad 4: TRACK_CYCLE (handled by PERF2 mode directly)
     * - Pads 5-6: CLIP mode (mute/arm)
     * - Pads 7-9: USER mode
     * 
     * @param sourcePage The page/subsystem requesting the LED update
     * @param padIndex The pad index (0-9)
     * @return true if this LED update should be rendered in PERF2 mode
     */
    private boolean shouldRenderLedInPerf2Mode(Page sourcePage, int padIndex) {
        switch (sourcePage) {
            case CLIP:
                // CLIP mode controls pads 5-6 in PERF2 mode (mute/arm)
                return (padIndex == 5 || padIndex == 6);
            
            case USER:
                // USER mode controls pads 2-3 and 7-9 in PERF2 mode
                return (padIndex == 2 || padIndex == 3) || (padIndex >= 7 && padIndex <= 9);
            
            case PERF2:
                // PERF2 mode can update any pad (for focused clip, smart assistant, BWS track cycle, etc.)
                return true;
            
            default:
                // Unknown source page - allow for backward compatibility
                return true;
        }
    }

    public void exit() {
        apiManager.exit();

        softstepHardware.exit();
    }

}
