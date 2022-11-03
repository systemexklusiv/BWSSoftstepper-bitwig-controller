package de.davidrival.softstep.hardware;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum LedLight {
    OFF(0),
    ON(1),
    BLINK(2),
    FAST_BLINK(3),
    FLASH(4);

    public final int data2ForLed;

}
