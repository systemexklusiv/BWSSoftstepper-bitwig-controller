package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.*;
import de.davidrival.softstep.controller.SoftstepController;
import lombok.Getter;
import lombok.Setter;

import static de.davidrival.softstep.api.ApiManager.PLAYBACK_EVENT.*;


@Getter
@Setter
public class ApiManager {

    private final ClipLauncherSlotBank slotBank;
    private UserControlBank userControls;
    private SoftstepController softstepController;
    public enum PLAYBACK_EVENT {STOPPED, PLAYING, RECORDING, PLAYBACK_STATE_NOT_KNOWN}

    public ApiManager(ControllerHost host) {
         this.userControls = host.createUserControls(10);
         TrackBank trackBank = host.createMainTrackBank(1,0, 9);
         trackBank.setShouldShowClipLauncherFeedback(true);
         Track track = trackBank.getItemAt(0);
         this.slotBank = track.clipLauncherSlotBank();

        this.slotBank.addHasContentObserver((slotIndex, onOff) -> softstepController
                .contentInSlotBankChanged( slotIndex, onOff) );

        this.slotBank.addPlaybackStateObserver((slotIndex, playbackState, isQueued) -> softstepController
                .playbackStateChanged(slotIndex, getApiEventByCallbackIndex(playbackState)
                        ,  isQueued));
    }

    private PLAYBACK_EVENT getApiEventByCallbackIndex(int playbackState) {
        switch (playbackState) {
            case 0: return STOPPED;
            case 1: return PLAYING;
            case 2: return RECORDING;
            default: return PLAYBACK_STATE_NOT_KNOWN;
        }
    }
    public void setController(SoftstepController softstepController) {
        this.softstepController = softstepController;
    }
}
