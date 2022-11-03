package de.davidrival.softstep.hardware;

import com.bitwig.extension.controller.api.MidiOut;
import de.davidrival.softstep.controller.Pages;

import java.util.concurrent.atomic.AtomicInteger;

public class SoftstepHardware extends SoftstepHardwareBase{
    public SoftstepHardware(MidiOut midiOut) {
        super(midiOut);
    }

    public void showLeds(Pages currentPage) {
        AtomicInteger i = new AtomicInteger();
        currentPage.ledStates.forEach(
                led -> setLed(i.getAndIncrement()
                        , led.ledColor.data2ForLed
                        , led.ledFlashing.data2ForLed)
        );
    }
}
