package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.*;
import de.davidrival.softstep.controller.SoftstepController;
import lombok.Getter;
import lombok.Setter;

import static de.davidrival.softstep.api.ApiManager.PLAYBACK_EVENT.*;


@Getter
@Setter
public class ApiManager {

    public static final int AMOUNT_USER_CONTROLS = 10;
    public static final int NUM_TRACKS = 1;
    public static final int NUM_SENDS = 0;
    public static final int NUM_SCENES = 9;
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
        this.slotBank.addHasContentObserver((slotIndex, onOff) -> softstepController
                .contentInSlotBankChanged(slotIndex, onOff));

        this.slotBank.addPlaybackStateObserver((slotIndex, playbackState, isQueued) -> softstepController
                .playbackStateChanged(slotIndex, getApiEventByCallbackIndex(playbackState)
                        , isQueued));
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
                return PLAYBACK_STATE_NOT_KNOWN;
        }
    }

    public void setController(SoftstepController softstepController) {
        this.softstepController = softstepController;
    }

}
