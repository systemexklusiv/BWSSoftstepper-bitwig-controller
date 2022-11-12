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
        public static final LedStates FOOT_ON = new LedStates(LedColor.RED, LedLight.ON);
        public static final LedStates FOOT_OFF = new LedStates(LedColor.GREEN, LedLight.ON);
    }

    public static class PAD_INDICES {
        public static final int MUTE_PAD = 5;
        public static final int ARM_PAD = 6;
        public static final int NAV_LEFT = 7;
        public static final int NAV_RIGHT = 8;
        public static final int NAV_UP = 9;
        public static final int NAV_DOWN = 4;
    }


}
