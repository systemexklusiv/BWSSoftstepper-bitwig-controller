package de.davidrival.softstep.hardware;

import com.bitwig.extension.controller.api.MidiOut;
import de.davidrival.softstep.controller.LedStates;
import de.davidrival.softstep.controller.Page;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SoftstepHardware extends SoftstepHardwareBase{

    public SoftstepHardware(MidiOut midiOut) {
        super(midiOut);
    }

    public void showAllLeds(Page currentPage) {
        resetLeds();
        AtomicInteger i = new AtomicInteger();
        currentPage.ledStates.forEach(
                led -> super.setLed(i.getAndIncrement()
                        , led.ledColor.data2ForLed
                        , led.ledFlashing.data2ForLed)
        );
    }

    public void showPageLeds(ArrayList<LedStates> ledStateBuffer) {
        resetLeds();
        AtomicInteger i = new AtomicInteger();
        ledStateBuffer.forEach(
                led -> super.setLed(i.getAndIncrement()
                        , led.ledColor.data2ForLed
                        , led.ledFlashing.data2ForLed)
        );
    }


}
