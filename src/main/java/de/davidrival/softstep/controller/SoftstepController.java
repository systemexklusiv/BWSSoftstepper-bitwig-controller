package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Parameter;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.hardware.SoftstepHardware;

import static de.davidrival.softstep.controller.Page.CLIP_LED_STATES.*;

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
        softstepHardware.showInitialLeds(pages.getCurrentPage());
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
//    public void checkBitwigToController

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

    public void contentInSlotBankChanged(int idx, boolean onOff) {
                    updateLedStates(Page.CLIP, idx, onOff ? Page.CLIP_LED_STATES.STOP : OFF);
                    p("! content ! " + onOff);
    }

    public void playbackStateChanged(int slotIndex, ApiManager.PLAYBACK_EVENT playbackEvent, boolean isQueued) {
        p("! playbackStateChanged ! slotIndex " + slotIndex + " playbackState " + playbackEvent.toString() + " isQueued " + isQueued);
        switch (playbackEvent) {
            case STOPPED:
                updateLedStates(Page.CLIP, slotIndex, isQueued ? STOP_QUE : STOP);
                break;
            case PLAYING:
                updateLedStates(Page.CLIP, slotIndex, isQueued ? PLAY_QUE : PLAY);
                break;
            case RECORDING:
                updateLedStates(Page.CLIP, slotIndex, isQueued ? REC_QUE : REC);
                break;
        }
    }

    /**
     *
     * @param slotIndex
     * @param ledStates
     */
    private void updateLedStates(Page page, int slotIndex, LedStates ledStates) {

//        pages.distributeLedStates();

        softstepHardware.drawLedAt(slotIndex, ledStates);
    }

    public void exit() {
        softstepHardware.exit();
    }

    public void p(String text) {
        apiManager.getHost().println(text);
    }
}
