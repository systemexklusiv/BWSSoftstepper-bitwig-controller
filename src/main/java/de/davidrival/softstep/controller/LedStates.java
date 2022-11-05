package de.davidrival.softstep.controller;

import de.davidrival.softstep.hardware.LedColor;
import de.davidrival.softstep.hardware.LedLight;
import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
public class LedStates {
    public final LedColor ledColor;
    public final LedLight ledFlashing;
}
