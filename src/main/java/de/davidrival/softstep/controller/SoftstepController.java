package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.BaseConsolePrinter;
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

    private ControllerPages pages;

    private List<HasControllsForPage> hasControllsForPages;

    public SoftstepController(
            ControllerPages controllerPages
            , SoftstepHardware softstepHardware
            , ControllerHost hostOrNull
            , PadConfigurationManager padConfigManager) {

        super(hostOrNull);
        this.pages = controllerPages;
        this.softstepHardware = softstepHardware;
        this.apiManager = new ApiManager(hostOrNull, this);

        this.controls = new Controls(apiManager.getHost());

        hasControllsForPages = new ArrayList<>();
        HasControllsForPage clipControlls = new ClipControls(Page.CLIP, apiManager);
        HasControllsForPage userControlls = new UserControlls(Page.USER, apiManager, padConfigManager);
        HasControllsForPage perfPage = new PerfConsolePrinter(Page.PERF, apiManager, padConfigManager);
        hasControllsForPages.add(clipControlls);
        hasControllsForPages.add(userControlls);
        hasControllsForPages.add(perfPage);

    }

    public void display() {
        softstepHardware.displayText(pages.getCurrentPage().name());
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
        // Cycle through all three available pages: CLIP → USER → PERF → CLIP
        if (pages.getCurrentPage().equals(Page.CLIP)) {
            pages.setCurrentPage(Page.USER);
        } else if (pages.getCurrentPage().equals(Page.USER)) {
            pages.setCurrentPage(Page.PERF);
        } else {
            pages.setCurrentPage(Page.CLIP);
        }
        display();
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
     * Updates LED states for PERF mode, which needs special handling since it's a hybrid mode.
     * PERF mode uses CLIP functionality for pads 0-3,5 and USER functionality for pads 4,6-9.
     * This method ensures LEDs are updated correctly regardless of which internal page system
     * is calling the update.
     * 
     * @param page The original page making the LED update call (Page.CLIP or Page.USER)
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
                p(String.format("PERF Mode LED Update: Pad %d from %s page with state %s", 
                    index, page.toString(), ledStates.toString()));
            } else {
                // Debug logging for blocked updates
                p(String.format("PERF Mode LED BLOCKED: Pad %d from %s page (assigned to different subsystem)", 
                    index, page.toString()));
            }
        }
        // If not in PERF mode, fall back to normal behavior
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

    public void exit() {
        apiManager.exit();

        softstepHardware.exit();
    }

}
