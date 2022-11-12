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
            , ControllerHost hostOrNull) {

        super(hostOrNull);
        this.pages = controllerPages;
        this.softstepHardware = softstepHardware;
        this.apiManager = new ApiManager(hostOrNull, this);

        this.controls = new Controls(apiManager.getHost());

        hasControllsForPages = new ArrayList<>();
        HasControllsForPage clipControlls = new ClipControls(Page.CLIP, apiManager);
        HasControllsForPage userControlls = new UserControlls(Page.USER, apiManager);
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
        if (checkPedal(msg)) return;

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

    // in hosted mode the NavPAd uo and down can just be configured as inc dec
    // therefor I store the current data2 and is the next is greater I swap to clip
    // otherwise to control mode
    private int valueStore = -1;
    private boolean isMidiUsedForPageChange(ShortMidiMessage msg) {
        if (msg.getStatusByte() == SoftstepHardwareBase.STATUS_BYTE) {
                if (msg.getData1() == SoftstepHardwareBase.NAVIGATION_DATA1) {

                    if (valueStore == -1) {
                        valueStore = msg.getData2();
                        return true;
                    }

                    if (msg.getData2() > valueStore && !pages.getCurrentPage().equals(Page.CLIP)) {
                        pages.setCurrentPage(Page.CLIP);
                        display();

                        return true;
                    }
                    if (msg.getData2() < valueStore && !pages.getCurrentPage().equals(Page.USER)) {
                        pages.setCurrentPage(Page.USER);
                        display();
                        return true;
                    }
                }
                valueStore = msg.getData2();
        }

        return false;
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
        softstepHardware.exit();
    }

}
