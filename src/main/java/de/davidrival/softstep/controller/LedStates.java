package de.davidrival.softstep.controller;

import de.davidrival.softstep.hardware.LedColor;
import de.davidrival.softstep.hardware.LedLight;
import lombok.Builder;

@Builder
public class LedStates {
    public final LedColor ledColor;
    public final LedLight ledFlashing;
}