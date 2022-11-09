package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.*;
import de.davidrival.softstep.controller.Page;
import de.davidrival.softstep.controller.SoftstepController;
import lombok.Getter;
import lombok.Setter;

import java.util.Timer;
import java.util.TimerTask;

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

    private final CursorTrack trackCurser;
    private final ApiHostToController apiFromHost;
    private final ApiControllerToHost apiToHost;

    private Timer timer;

    public enum PLAYBACK_EVENT {STOPPED, PLAYING, RECORDING, PLAYBACK_STATE_NOT_KNOWN}

    private final ClipLauncherSlotBank slotBank;
    private UserControlBank userControls;
    private TrackBank trackBank;
    private Track track;

    private final SceneBank sceneBank;

    private SoftstepController softstepController;
    private ControllerHost host;


    public ApiManager(ControllerHost host, SoftstepController softstepController) {

        this.host = host;
        this.softstepController = softstepController;

        this.userControls = host.createUserControls(AMOUNT_USER_CONTROLS);
        this.trackBank = host.createMainTrackBank(NUM_TRACKS, NUM_SENDS, NUM_SCENES);
        this.trackCurser = host.
                createCursorTrack("SOFTSTEP_CURSER_TRACK"
                        , "softstep curster"
                        ,0
                        ,0
                        ,true
                ) ;
        this.sceneBank = trackBank.sceneBank();
        this.trackBank.setShouldShowClipLauncherFeedback(SHOW_CLIP_LAUNCHER_FEEDBACK);
        this.track = trackBank.getItemAt(0);


        this.trackBank.followCursorTrack(trackCurser);

        this.slotBank = track.clipLauncherSlotBank();

        this.apiFromHost = new ApiHostToController(this);
        this.apiToHost = new ApiControllerToHost(this);

        this.slotBank.addHasContentObserver(apiFromHost::onContentInSlotBankChanged);

        this.slotBank.addPlaybackStateObserver(apiFromHost::onPlaybackStateChanged);

    }

}
