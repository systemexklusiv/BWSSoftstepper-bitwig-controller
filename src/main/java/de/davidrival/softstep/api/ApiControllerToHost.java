package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Parameter;
import de.davidrival.softstep.controller.Page;
import de.davidrival.softstep.controller.SoftstepController;

import java.util.Timer;

import static de.davidrival.softstep.api.ApiManager.USER_CONTROL_PARAMETER_RESOLUTION;
import static de.davidrival.softstep.controller.Page.CLIP_LED_STATES.OFF;
import static de.davidrival.softstep.controller.Page.CLIP_LED_STATES.STOP;

public class ApiControllerToHost extends SimpleConsolePrinter{

    private final ApiManager api;

    public ApiControllerToHost(ApiManager api) {
        super(api.getHost());
        this.api = api;
    }

    public void fireSlotAt(int number) {
        api.getSlotBank()
                .launch(number);
    }

    public void deleteSlotAt(int number) {
        api.getSlotBank()
                .getItemAt(number)
                .deleteObject();
    }

    public void setValueOfUserControl(int index, int value) {
        Parameter parameter = api.getUserControls()
                .getControl(index);
        parameter.set(value, USER_CONTROL_PARAMETER_RESOLUTION);
    }

    public void clipSlotBankDown() {
//        p("clipSlotBankUp");
        api.getTrackBank().scrollForwards();
        api.getTrack().selectInMixer();
    }

    public void clipSlotBankUp() {
//        p("clipSlotBankDown");
        api.getTrackBank().scrollBackwards();
        api.getTrack().selectInMixer();
    }

    public void clipSlotBankLeft() {
//        p("clipSlotBankLeft");
        api.getSceneBank().scrollByPages(-1);
    }

    public void clipSlotBankRight() {
//        p("clipSlotBankRight");
        api.getSceneBank().scrollByPages(1);

    }

    public void armTrack(int number) {
        api.getTrackCurser().arm().toggle();
    }

    public void muteTrack(int number) {
        api.getTrackCurser().mute().toggle();
    }

    public void stopTrack(int number) {
        api.getTrackCurser().stop();
    }
}
