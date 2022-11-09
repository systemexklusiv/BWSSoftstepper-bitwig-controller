package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.*;
import de.davidrival.softstep.controller.Page;
import de.davidrival.softstep.controller.SoftstepController;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static de.davidrival.softstep.api.ApiManager.PLAYBACK_EVENT.*;
import static de.davidrival.softstep.controller.Page.CLIP_LED_STATES.*;

@Getter
@Setter
public class ApiManager {

    public static final int AMOUNT_USER_CONTROLS = 10;
    public static final int NUM_TRACKS = 1;
    public static final int NUM_SENDS = 0;
    public static final int NUM_SCENES = 4;
    public static final boolean SHOW_CLIP_LAUNCHER_FEEDBACK = true;
    public static final int USER_CONTROL_PARAMETER_RESOLUTION = 128;
    public static final int CLIPS_CONTENT_CLEANUP_PERIOD = 2000;

    private Timer timer;

    public enum PLAYBACK_EVENT {STOPPED, PLAYING, RECORDING, PLAYBACK_STATE_NOT_KNOWN}

    private final ClipLauncherSlotBank slotBank;
    private UserControlBank userControls;
    private TrackBank trackBank;
    private Track track;

    private final SceneBank sceneBank;

    private SoftstepController softstepController;
    private ControllerHost host;

    public ApiManager(ControllerHost host) {

        this.host = host;
        this.userControls = host.createUserControls(AMOUNT_USER_CONTROLS);
        this.trackBank = host.createMainTrackBank(NUM_TRACKS, NUM_SENDS, NUM_SCENES);
        this.sceneBank = trackBank.sceneBank();
        trackBank.setShouldShowClipLauncherFeedback(SHOW_CLIP_LAUNCHER_FEEDBACK);
        track = trackBank.getItemAt(0);
        this.slotBank = track.clipLauncherSlotBank();
        this.slotBank.addHasContentObserver(this::contentInSlotBankChanged);

        this.slotBank.addPlaybackStateObserver((slotIndex, playbackState, isQueued) -> playbackStateChanged(slotIndex, getApiEventByCallbackIndex(playbackState)
                , isQueued));

        /* Import or some content updates are not correct */
         runClipCleanupTaskEach(CLIPS_CONTENT_CLEANUP_PERIOD);
    }

    public void fireSlotAt(int number) {
        slotBank
                .launch(number);
    }

    public void deleteSlotAt(int number) {
        slotBank
                .getItemAt(number)
                .deleteObject();
    }

    public void setValueOfUserControl(int index, int value) {
        Parameter parameter = userControls
                .getControl(index);
        parameter.set(value, USER_CONTROL_PARAMETER_RESOLUTION);
    }

    public void clipSlotBankDown() {
        p("clipSlotBankUp");
        trackBank.scrollForwards();
        track.selectInMixer();
    }

    public void clipSlotBankUp() {
        p("clipSlotBankDown");
        trackBank.scrollBackwards();
        track.selectInMixer();
    }

    public void clipSlotBankLeft() {
        p("clipSlotBankLeft");
        sceneBank.scrollByPages(-1);

    }

    public void clipSlotBankRight() {
        p("clipSlotBankRight");
        sceneBank.scrollByPages(1);

    }

    /**
     * checks for content in clip slots infinitly ond if absents sends explicitly a
     * OFF LED at the specific point. This is a fix or sometimes LED get Stuck
     *
     * @param millis time the task repeats
     */
    private void runClipCleanupTaskEach(int millis) {
        timer = new Timer();
        int size = slotBank.getSizeOfBank();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                p(">>> running cleanup :-)");
                for (int i = 0; i < size; i++) {
                    ClipLauncherSlot clipLauncherSlot = slotBank.getItemAt(i);
                    if ( !clipLauncherSlot.hasContent().get() ){
                        softstepController.updateLedStates(Page.CLIP, i, OFF);
                    }
                }
            }
        }, 5000, millis);
    }

    public void contentInSlotBankChanged(int idx, boolean onOff) {
        p("! content ! slotIdx" + idx + " clip? " + onOff);
        softstepController.updateLedStates(Page.CLIP, idx, onOff ? STOP : OFF);
    }

    public void playbackStateChanged(int slotIndex, ApiManager.PLAYBACK_EVENT playbackEvent, boolean isQueued) {
//        p("! playbackStateChanged ! slotIndex " + slotIndex + " playbackState " + playbackEvent.toString() + " isQueued " + isQueued);
        switch (playbackEvent) {
            case STOPPED:
                softstepController.updateLedStates(Page.CLIP, slotIndex, isQueued ? STOP_QUE : STOP);
                break;
            case PLAYING:
                softstepController.updateLedStates(Page.CLIP, slotIndex, isQueued ? PLAY_QUE : PLAY);
                break;
            case RECORDING:
                softstepController.updateLedStates(Page.CLIP, slotIndex, isQueued ? REC_QUE : REC);
                break;
        }
    }

    private PLAYBACK_EVENT getApiEventByCallbackIndex(int playbackState) {
        switch (playbackState) {
            case 0:
                return STOPPED;
            case 1:
                return PLAYING;
            case 2:
                return RECORDING;
            default:
                host.errorln("Unknown state from PlaybackStateObserver with idx: " + playbackState);
                return PLAYBACK_STATE_NOT_KNOWN;
        }
    }

    public void setController(SoftstepController softstepController) {
        this.softstepController = softstepController;
    }

    public void p(String text) {
        host.println(text);
    }
}
