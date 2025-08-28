package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.SimpleConsolePrinter;
import de.davidrival.softstep.hardware.SoftstepHardware;

import de.davidrival.softstep.hardware.SoftstepHardwareBase;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Getter
@Setter
public class SoftstepController extends SimpleConsolePrinter {

    public static final int USER_CONTROL_INDEX_FOR_PEDAL = 10;
    public static final int PEDAL_DATA1 = 50;
    public static final double PEDAL_DATA2_MULTI = 1.95;

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
        hasControllsForPages.add(clipControlls);
        hasControllsForPages.add(userControlls);

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
        // Simple cycle between the two available pages
        if (pages.getCurrentPage().equals(Page.CLIP)) {
            pages.setCurrentPage(Page.USER);
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

    public void exit() {
        apiManager.exit();

        softstepHardware.exit();
    }

}
