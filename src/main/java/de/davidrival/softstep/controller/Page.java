package de.davidrival.softstep.controller;

import de.davidrival.softstep.hardware.LedColor;
import de.davidrival.softstep.hardware.LedLight;
import lombok.ToString;


import java.util.ArrayList;
import java.util.Arrays;

@ToString
public enum Page {
    USER(0
            , new LedStates(LedColor.RED, LedLight.ON)
            , new LedStates(LedColor.GREEN, LedLight.ON)
            , new ArrayList<>(Arrays.asList(
            new LedStates(LedColor.GREEN, LedLight.ON),
            new LedStates(LedColor.GREEN, LedLight.ON),
            new LedStates(LedColor.GREEN, LedLight.ON),
            new LedStates(LedColor.GREEN, LedLight.ON),
            new LedStates(LedColor.GREEN, LedLight.ON),
            new LedStates(LedColor.GREEN, LedLight.ON),
            new LedStates(LedColor.GREEN, LedLight.ON),
            new LedStates(LedColor.GREEN, LedLight.ON),
            new LedStates(LedColor.GREEN, LedLight.ON),
            new LedStates(LedColor.GREEN, LedLight.ON)
    ))),

    CLIP(1
            , new LedStates(LedColor.YELLOW, LedLight.OFF)
            , new LedStates(LedColor.YELLOW, LedLight.OFF)
            , new ArrayList<>(Arrays.asList(
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.GREEN, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.RED, LedLight.OFF),
                new LedStates(LedColor.GREEN, LedLight.OFF))
            )
    ),

    PERF(2
            , new LedStates(LedColor.YELLOW, LedLight.ON)
            , new LedStates(LedColor.YELLOW, LedLight.OFF)
            , new ArrayList<>(Arrays.asList(
                // Pads 0-3: CLIP mode colors (clips)
                new LedStates(LedColor.YELLOW, LedLight.OFF),  // Pad 0
                new LedStates(LedColor.YELLOW, LedLight.OFF),  // Pad 1
                new LedStates(LedColor.YELLOW, LedLight.OFF),  // Pad 2
                new LedStates(LedColor.YELLOW, LedLight.OFF),  // Pad 3
                // Pad 4: USER mode color (configurable)
                new LedStates(LedColor.GREEN, LedLight.ON),    // Pad 4
                // Pad 5: CLIP mode color (track control)
                new LedStates(LedColor.GREEN, LedLight.OFF),   // Pad 5
                // Pads 6-9: USER mode colors (configurable)
                new LedStates(LedColor.GREEN, LedLight.ON),    // Pad 6
                new LedStates(LedColor.GREEN, LedLight.ON),    // Pad 7
                new LedStates(LedColor.GREEN, LedLight.ON),    // Pad 8
                new LedStates(LedColor.GREEN, LedLight.ON)     // Pad 9
            ))
    );

    public final int pageIndex;
    public final LedStates on;
    public final LedStates off;
    public final ArrayList<LedStates> ledStates;


    Page(int pageIndex, LedStates on, LedStates off, ArrayList<LedStates> initialLedStates) {
        this.pageIndex = pageIndex;
        this.on = on;
        this.off = off;
        this.ledStates = initialLedStates;
    }


    public static class CLIP_LED_STATES {
        public static final LedStates OFF = new LedStates(LedColor.YELLOW, LedLight.OFF);
        public static final LedStates STOP = new LedStates(LedColor.YELLOW, LedLight.ON);
        public static final LedStates STOP_QUE = new LedStates(LedColor.YELLOW, LedLight.BLINK);
        public static final LedStates PLAY = new LedStates(LedColor.GREEN, LedLight.ON);
        public static final LedStates PLAY_QUE = new LedStates(LedColor.GREEN, LedLight.BLINK);
        public static final LedStates REC = new LedStates(LedColor.RED, LedLight.ON);
        public static final LedStates REC_QUE = new LedStates(LedColor.RED, LedLight.BLINK);
    }
    public static class CHANNEL_LED_STATES {
        public static final LedStates MUTED = new LedStates(LedColor.YELLOW, LedLight.BLINK);
        public static final LedStates UNMUTED = new LedStates(LedColor.YELLOW, LedLight.OFF);
        public static final LedStates ARMED = new LedStates(LedColor.RED, LedLight.ON);
        public static final LedStates UNARMED = new LedStates(LedColor.GREEN, LedLight.ON);
    }
    public static class USER_LED_STATES {
        // Legacy constants (kept for compatibility)
        public static final LedStates FOOT_ON = new LedStates(LedColor.RED, LedLight.ON);
        public static final LedStates FOOT_OFF = new LedStates(LedColor.GREEN, LedLight.ON);
        
        // Enhanced mode-specific LED states
        
        // TOGGLE Mode
        public static final LedStates TOGGLE_OFF = new LedStates(LedColor.YELLOW, LedLight.ON);  // Orange when OFF
        public static final LedStates TOGGLE_ON = new LedStates(LedColor.RED, LedLight.ON);     // Red when ON
        
        // INCREMENT Mode  
        public static final LedStates INCREMENT_MIN = new LedStates(LedColor.GREEN, LedLight.ON);   // Green at minimum
        public static final LedStates INCREMENT_MID = new LedStates(LedColor.YELLOW, LedLight.ON);  // Orange in between
        public static final LedStates INCREMENT_MAX = new LedStates(LedColor.RED, LedLight.ON);     // Red at maximum/wraparound
        
        // MOMENTARY Mode
        public static final LedStates MOMENTARY_RELEASED = new LedStates(LedColor.GREEN, LedLight.ON);  // Green when released
        public static final LedStates MOMENTARY_PRESSED = new LedStates(LedColor.RED, LedLight.ON);    // Red when pressed
        
        // PRESSURE Mode
        public static final LedStates PRESSURE_RELEASED = new LedStates(LedColor.GREEN, LedLight.ON);  // Green when released
        public static final LedStates PRESSURE_PRESSED = new LedStates(LedColor.RED, LedLight.ON);    // Red when pressed
        
        // Special States
        public static final LedStates LONG_PRESS_FLASH = new LedStates(LedColor.YELLOW, LedLight.FLASH);  // Yellow flash for long press
        public static final LedStates DISABLED = new LedStates(LedColor.GREEN, LedLight.OFF);           // Off state
        
        // BWS TRACK_CYCLE Mode - Shows which BWS track is currently active
        public static final LedStates BWS_TRACK_0 = new LedStates(LedColor.YELLOW, LedLight.ON);         // BWS:0 - Yellow solid
        public static final LedStates BWS_TRACK_1 = new LedStates(LedColor.YELLOW, LedLight.BLINK);      // BWS:1 - Yellow blink
        public static final LedStates BWS_TRACK_2 = new LedStates(LedColor.YELLOW, LedLight.FAST_BLINK); // BWS:2 - Yellow fast blink
        public static final LedStates BWS_TRACK_3 = new LedStates(LedColor.RED, LedLight.ON);            // BWS:3 - Red solid
        public static final LedStates BWS_TRACK_4 = new LedStates(LedColor.RED, LedLight.BLINK);         // BWS:4 - Red blink
        public static final LedStates BWS_TRACK_5 = new LedStates(LedColor.RED, LedLight.FAST_BLINK);    // BWS:5 - Red fast blink
        public static final LedStates BWS_INACTIVE = new LedStates(LedColor.GREEN, LedLight.OFF);        // No BWS tracks available
        public static final LedStates BWS_NON_BWS_TRACK = new LedStates(LedColor.GREEN, LedLight.BLINK); // Non-BWS track selected
    }

    public static class PAD_INDICES {
        public static final int MUTE_PAD = 5;
        public static final int ARM_PAD = 6;
        public static final int NAV_LEFT = 7;
        public static final int NAV_RIGHT = 8;
        public static final int NAV_UP = 9;      // Fixed: Top right pad goes UP
        public static final int NAV_DOWN = 4;    // Fixed: Bottom right pad goes DOWN
    }


}
