package de.davidrival.softstep.hardware;

import com.bitwig.extension.controller.api.MidiOut;
import de.davidrival.softstep.controller.LedStates;
import de.davidrival.softstep.controller.Pages;

import java.util.concurrent.atomic.AtomicInteger;

public class SoftstepHardware extends SoftstepHardwareBase{

    public SoftstepHardware(MidiOut midiOut) {
        super(midiOut);
    }

    public void showLeds(Pages currentPage) {
        resetLeds();
        AtomicInteger i = new AtomicInteger();
        currentPage.initialLedStates.forEach(
                led -> super.setLed(i.getAndIncrement()
                        , led.ledColor.data2ForLed
                        , led.ledFlashing.data2ForLed)
        );
    }

    public void drawLedAt(int index, LedStates ledStates) {
        super.setLed(index
                , ledStates.ledColor.data2ForLed
                , ledStates.ledFlashing.data2ForLed);
    }
}
