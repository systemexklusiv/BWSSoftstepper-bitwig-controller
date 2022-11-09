package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.Parameter;
import de.davidrival.softstep.controller.Page;

import static de.davidrival.softstep.api.ApiManager.USER_CONTROL_PARAMETER_RESOLUTION;

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

        // update LEDs not for pedal which is user controll 11
        if (index < 10) {
            api.getSoftstepController().getSoftstepHardware().drawFastAt( index, value > 0
                            ? Page.USER_LED_STATES.FOOT_ON
                            : Page.USER_LED_STATES.FOOT_OFF);

        }
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

    public void armTrack() {
        api.getTrackCurser().arm().toggle();
    }

    public void muteTrack() {
        api.getTrackCurser().mute().toggle();
    }

    public void stopTrack() {
        api.getTrackCurser().stop();
    }
}
