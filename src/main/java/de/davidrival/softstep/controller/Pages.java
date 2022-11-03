package de.davidrival.softstep.controller;

import de.davidrival.softstep.hardware.LedColor;
import de.davidrival.softstep.hardware.LedFlashing;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public enum Pages {
    CTRL(0, Arrays.asList(
            new LedStates(LedColor.RED, LedFlashing.ON),
            new LedStates(LedColor.RED, LedFlashing.ON),
            new LedStates(LedColor.RED, LedFlashing.ON),
            new LedStates(LedColor.RED, LedFlashing.ON),
            new LedStates(LedColor.RED, LedFlashing.ON),
            new LedStates(LedColor.RED, LedFlashing.ON),
            new LedStates(LedColor.RED, LedFlashing.ON),
            new LedStates(LedColor.RED, LedFlashing.ON),
            new LedStates(LedColor.RED, LedFlashing.ON),
            new LedStates(LedColor.RED, LedFlashing.ON)
    )),
    CLIP(1, Arrays.asList(
            new LedStates(LedColor.YELLOW, LedFlashing.ON),
            new LedStates(LedColor.YELLOW, LedFlashing.ON),
            new LedStates(LedColor.YELLOW, LedFlashing.ON),
            new LedStates(LedColor.YELLOW, LedFlashing.ON),
            new LedStates(LedColor.YELLOW, LedFlashing.ON),
            new LedStates(LedColor.YELLOW, LedFlashing.ON),
            new LedStates(LedColor.YELLOW, LedFlashing.ON),
            new LedStates(LedColor.YELLOW, LedFlashing.ON),
            new LedStates(LedColor.YELLOW, LedFlashing.ON),
            new LedStates(LedColor.YELLOW, LedFlashing.ON)
    ));
    public final int pageIndex;
    public final List<LedStates> ledStates;



}
