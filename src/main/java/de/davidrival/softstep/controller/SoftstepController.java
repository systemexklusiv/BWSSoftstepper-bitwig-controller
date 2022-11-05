package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Parameter;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.hardware.SoftstepHardware;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;


@Getter
@Setter
public class SoftstepController {

    private static final int MODE_THRESHOLD = 0;

    private SoftstepHardware softstepHardware;

    final Softstep1Controls controls = new Softstep1Controls();

    private ApiManager apiManager;

    private ControllerHost controllerHost;

    private ControllerPages pages;

    public SoftstepController(
            ControllerPages controllerPages
            , SoftstepHardware softstepHardware
            , ApiManager apiManager) {

        this.pages = controllerPages;
        this.softstepHardware = softstepHardware;
        this.apiManager = apiManager;
        this.apiManager.setController(this);
    }

    public void display() {
        softstepHardware.displayText(pages.getCurrentPage().name());
        softstepHardware.showAllLeds(pages.getCurrentPage());
    }

    public void handleMidi(ShortMidiMessage msg) {
        checkForPageChange(msg);
        controls.update(msg);
        checkApiToBitwig();
    }

    private void checkApiToBitwig() {
        List<Softstep1Pad> pads = controls.getPads()
                .stream().filter(pad -> pad.hasChanged)
                .collect(Collectors.toList());

        switch (pages.getCurrentPage()) {
            case CTRL:
                pads.forEach(pad -> {
                            Parameter param = apiManager
                                    .getUserControls()
                                    .getControl(pad.getNumber());
                            param.set(pad.pressure, 128);
                            pad.hasChanged = false;
                        }
                );
                break;
            case CLIP:
                pads.forEach(pad -> {
                            apiManager
                                    .getSlotBank()
                                    .launch(pad.getNumber());
                            pad.hasChanged = false;
                        }
                );
                break;
        }
    }

    private void checkForPageChange(ShortMidiMessage msg) {
        if (msg.getStatusByte() == 176 && msg.getData1() == 80 && msg.getData2() > MODE_THRESHOLD) {
            if (pages.getCurrentPage().pageIndex != Page.CLIP.pageIndex) {
                pages.setCurrentPage(Page.CLIP);
                display();
            }
        }
        if (msg.getStatusByte() == 176 && msg.getData1() == 81 && msg.getData2() > MODE_THRESHOLD) {
            if (pages.getCurrentPage().pageIndex != Page.CTRL.pageIndex) {
                pages.setCurrentPage(Page.CTRL);
                display();
            }
        }
    }


    /**
     * Write to the states of each page
     * Doesn't matter is currently active or not
     * Needed if one wants to switch to another mode. Afterwards it sends out the
     * changed led states to the control for the acive page
     *
     * @param page the page where the LED is to be set (not importand if active or not
     * @param index the index of the controller ( e.g. the slotindex if used in clip mode)
     * @param ledStates the led states, meaninf color and flashin mode
     */
    public void updateLedStates(Page page, int index, LedStates ledStates) {
        // First write to the states of each page
        // Doesn't matter is currently active or not
        // Needed if one wants to switch to another mode
        pages.distributeLedStates(page,index,ledStates);

        p(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
        p(Page.CLIP.ledStates.toString());
        p(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");

        // Only render the led states of the active page
        if (pages.getCurrentPage().equals(page)) {
            softstepHardware.drawLedAt(index, ledStates);
        }
    }

    public void exit() {
        softstepHardware.exit();
    }

    public void p(String text) {
        apiManager.getHost().println(text);
    }
}
