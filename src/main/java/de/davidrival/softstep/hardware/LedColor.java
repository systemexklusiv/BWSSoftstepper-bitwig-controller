package de.davidrival.softstep.hardware;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum LedColor {
    GREEN(0),
    RED(1),
    YELLOW(2);

    public final int data2ForLed;

}
