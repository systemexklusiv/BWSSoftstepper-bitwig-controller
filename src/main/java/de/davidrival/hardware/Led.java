package de.davidrival.hardware;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Led {
    OFF(0),
    ON(1),
    BLINK(2),
    FAST_BLINK(3),
    FLASH(4);

    public final int data2ForLed;

}
