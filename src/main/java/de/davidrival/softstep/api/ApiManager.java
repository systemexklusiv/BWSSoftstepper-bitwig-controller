package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.*;
import de.davidrival.softstep.controller.Page;
import de.davidrival.softstep.controller.SoftstepController;
import lombok.Getter;
import lombok.Setter;
import static de.davidrival.softstep.api.ApiManager.PLAYBACK_EVENT.*;
import static de.davidrival.softstep.controller.Page.CLIP_LED_STATES.*;

@Getter
@Setter
public class ApiManager {

    public static final int AMOUNT_USER_CONTROLS = 10;
    public static final int NUM_TRACKS = 1;
    public static final int NUM_SENDS = 0;
    public static final int NUM_SCENES = 5;
    public static final boolean SHOW_CLIP_LAUNCHER_FEEDBACK = true;

    public enum PLAYBACK_EVENT {STOPPED, PLAYING, RECORDING, PLAYBACK_STATE_NOT_KNOWN}

    private final ClipLauncherSlotBank slotBank;
    private UserControlBank userControls;
    private SoftstepController softstepController;
    private ControllerHost host;

    public ApiManager(ControllerHost host) {

        this.host = host;
        this.userControls = host.createUserControls(AMOUNT_USER_CONTROLS);
        TrackBank trackBank = host.createMainTrackBank(NUM_TRACKS, NUM_SENDS, NUM_SCENES);
        trackBank.setShouldShowClipLauncherFeedback(SHOW_CLIP_LAUNCHER_FEEDBACK);
        Track track = trackBank.getItemAt(0);
        this.slotBank = track.clipLauncherSlotBank();
        this.slotBank.addHasContentObserver(this::contentInSlotBankChanged);

        this.slotBank.addPlaybackStateObserver((slotIndex, playbackState, isQueued) -> playbackStateChanged(slotIndex, getApiEventByCallbackIndex(playbackState)
                        , isQueued));
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
