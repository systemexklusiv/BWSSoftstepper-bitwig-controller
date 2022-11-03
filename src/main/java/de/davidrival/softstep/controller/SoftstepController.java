package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import de.davidrival.softstep.hardware.SoftstepHardware;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@AllArgsConstructor
public class SoftstepController {

    Pages currentPage;
    private SoftstepHardware softstepHardware;

    public void display() {
        softstepHardware.displayText(currentPage.name());
        softstepHardware.showLeds(currentPage);
    }

    private static final int MODE_THRESHOLD = 0;
    public void handleMidi(ShortMidiMessage msg) {
        if ( msg.getStatusByte() == 176 && msg.getData1() == 80 && msg.getData2() > MODE_THRESHOLD) {
            if (currentPage.pageIndex != Pages.CLIP.pageIndex) {
                currentPage = Pages.CLIP;
                display();
            }
        }
        if ( msg.getStatusByte() == 176 && msg.getData1() == 81 && msg.getData2() > MODE_THRESHOLD ) {
            if (currentPage.pageIndex != Pages.CTRL.pageIndex) {
                currentPage = Pages.CTRL;
                display();
            }
        }
    }

    public void exit() {
        softstepHardware.exit();
    }

}
