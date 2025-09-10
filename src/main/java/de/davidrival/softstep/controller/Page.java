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
    ),
    
    PERF2(3
            , new LedStates(LedColor.YELLOW, LedLight.BLINK)  // Distinctive on state for PERF2
            , new LedStates(LedColor.YELLOW, LedLight.OFF)
            , new ArrayList<>(Arrays.asList(
                // Pad 0: Focused Clip Slot (OFF by default - will be dynamically updated)
                new LedStates(LedColor.YELLOW, LedLight.OFF),  // Pad 0 - Focused clip slot
                // Pad 1: Smart Recording Assistant (GREEN by default - ready to record)
                new LedStates(LedColor.GREEN, LedLight.ON),    // Pad 1 - Smart assistant
                // Pads 2-3: USER mode colors (freed up from clip slots)
                new LedStates(LedColor.GREEN, LedLight.ON),    // Pad 2 - USER mode
                new LedStates(LedColor.GREEN, LedLight.ON),    // Pad 3 - USER mode
                // Pad 4: BWS Track Cycle (same as PERF mode)
                new LedStates(LedColor.GREEN, LedLight.ON),    // Pad 4 - BWS track cycle
                // Pad 5: Track controls (same as PERF mode)
                new LedStates(LedColor.GREEN, LedLight.OFF),   // Pad 5 - Track control
                // Pads 6-9: USER mode colors (same as PERF mode)
                new LedStates(LedColor.GREEN, LedLight.ON),    // Pad 6 - USER mode
                new LedStates(LedColor.GREEN, LedLight.ON),    // Pad 7 - USER mode
                new LedStates(LedColor.GREEN, LedLight.ON),    // Pad 8 - USER mode
                new LedStates(LedColor.GREEN, LedLight.ON)     // Pad 9 - USER mode
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
    
    public static class PERF2_LED_STATES {
        // Smart Recording Assistant (Pad1) - Context-aware LED states
        public static final LedStates SMART_READY_TO_RECORD = new LedStates(LedColor.GREEN, LedLight.ON);        // Ready to start recording (when Pad0 empty)
        public static final LedStates SMART_UNDO_AVAILABLE = new LedStates(LedColor.RED, LedLight.BLINK);        // Undo recording available (when recording)
        public static final LedStates SMART_AUTO_ADVANCE = new LedStates(LedColor.GREEN, LedLight.BLINK);        // Auto-advance ready (when playing)
        public static final LedStates SMART_SCENE_CREATION = new LedStates(LedColor.GREEN, LedLight.FAST_BLINK); // Scene creation mode (no free slots)
        
        // Focused Clip Slot (Pad0) - Uses standard CLIP LED states but only shows current cursor position
        // These reference the existing CLIP_LED_STATES for consistency
        public static final LedStates FOCUSED_EMPTY = Page.CLIP_LED_STATES.OFF;     // Empty slot - OFF
        public static final LedStates FOCUSED_RECORDING = Page.CLIP_LED_STATES.REC; // Recording - RED + BLINK  
        public static final LedStates FOCUSED_PLAYING = Page.CLIP_LED_STATES.PLAY;  // Playing - GREEN (corrected)
        public static final LedStates FOCUSED_STOPPED = Page.CLIP_LED_STATES.STOP;  // Has content but stopped - YELLOW (corrected)
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
