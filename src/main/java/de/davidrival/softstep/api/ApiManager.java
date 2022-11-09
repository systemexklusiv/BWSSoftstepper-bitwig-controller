package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.*;
import de.davidrival.softstep.controller.Page;
import de.davidrival.softstep.controller.SoftstepController;
import lombok.Getter;
import lombok.Setter;

import java.util.Timer;
import java.util.TimerTask;

import static de.davidrival.softstep.controller.Page.CLIP_LED_STATES.OFF;

@Getter
@Setter
public class ApiManager {

    public static final int AMOUNT_USER_CONTROLS = 11;
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

        /* cleanup content updates which are not correct reported from time to time */
//        runClipCleanupTaskEach(CLIPS_CONTENT_CLEANUP_PERIOD);

        runPageCleanUpTask();
    }

    /**
     * checks for content in clip slots infinitly ond if absents sends explicitly a
     * OFF LED at the specific point. This is a fix or sometimes LED get Stuck
     */
    private void runClipCleanupTaskEach() {
        timer = new Timer();
        int size = getSlotBank().getSizeOfBank();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                p(">>> running cleanup :-)");
                for (int i = 0; i < size; i++) {
                    ClipLauncherSlot clipLauncherSlot = getSlotBank().getItemAt(i);
                    if ( !clipLauncherSlot.hasContent().get() ){
                        getSoftstepController().updateLedStates(Page.CLIP, i, OFF);
                    }
                }
            }
        }, 5000, CLIPS_CONTENT_CLEANUP_PERIOD);
    }

    private void runPageCleanUpTask() {
        timer = new Timer();
        Page current = getSoftstepController().getPages().getCurrentPage();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                getSoftstepController().getSoftstepHardware().showAllLeds(current);
            }
        }, 5000, CLIPS_CONTENT_CLEANUP_PERIOD);
    }

}
