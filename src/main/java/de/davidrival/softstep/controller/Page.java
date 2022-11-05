package de.davidrival.softstep.controller;

import de.davidrival.softstep.hardware.LedColor;
import de.davidrival.softstep.hardware.LedLight;

import java.util.ArrayList;
import java.util.Arrays;


public enum Page {
    CTRL(0
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
            , new LedStates(LedColor.YELLOW, LedLight.ON)
            , new LedStates(LedColor.YELLOW, LedLight.OFF)
            , new ArrayList<>(Arrays.asList(
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF),
                new LedStates(LedColor.YELLOW, LedLight.OFF))
            )
    );

    public final int pageIndex;
    public final LedStates on;
    public final LedStates off;
    public final ArrayList<LedStates> initialLedStates;


    Page(int pageIndex, LedStates on, LedStates off, ArrayList<LedStates> initialLedStates) {
        this.pageIndex = pageIndex;
        this.on = on;
        this.off = off;
        this.initialLedStates = initialLedStates;
    }


    static class CLIP_LED_STATES {
        public static final LedStates OFF = new LedStates(LedColor.YELLOW, LedLight.OFF);
        public static final LedStates STOP = new LedStates(LedColor.YELLOW, LedLight.ON);
        public static final LedStates STOP_QUE = new LedStates(LedColor.YELLOW, LedLight.BLINK);
        public static final LedStates PLAY = new LedStates(LedColor.GREEN, LedLight.ON);
        public static final LedStates PLAY_QUE = new LedStates(LedColor.GREEN, LedLight.BLINK);
        public static final LedStates REC = new LedStates(LedColor.RED, LedLight.ON);
        public static final LedStates REC_QUE = new LedStates(LedColor.RED, LedLight.BLINK);
    }

}
