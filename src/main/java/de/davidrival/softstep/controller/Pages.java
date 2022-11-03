package de.davidrival.softstep.controller;

import de.davidrival.softstep.hardware.LedColor;
import de.davidrival.softstep.hardware.LedLight;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public enum Pages {
    CTRL(0, Arrays.asList(
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
    )),
    CLIP(1, Arrays.asList(
            new LedStates(LedColor.YELLOW, LedLight.ON),
            new LedStates(LedColor.YELLOW, LedLight.ON),
            new LedStates(LedColor.YELLOW, LedLight.ON),
            new LedStates(LedColor.YELLOW, LedLight.ON),
            new LedStates(LedColor.YELLOW, LedLight.ON),
            new LedStates(LedColor.YELLOW, LedLight.ON),
            new LedStates(LedColor.YELLOW, LedLight.ON),
            new LedStates(LedColor.YELLOW, LedLight.ON),
            new LedStates(LedColor.YELLOW, LedLight.ON),
            new LedStates(LedColor.YELLOW, LedLight.ON)
    ));
    public final int pageIndex;
    public final List<LedStates> ledStates;



}
