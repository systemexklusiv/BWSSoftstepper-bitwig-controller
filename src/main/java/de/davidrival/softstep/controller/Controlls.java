package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.api.SimpleConsolePrinter;
import de.davidrival.softstep.hardware.SoftstepHardwareBase;
import lombok.Getter;
import lombok.Setter;

import java.util.*;


@Getter
@Setter
public class Controlls extends SimpleConsolePrinter {

    static final int START_DATA_1_AT = 44;
    List<Softstep1Pad> pads;

    ControllerHost host;

    public Controlls(ControllerHost hostOrNull) {
        super(hostOrNull);
        this.host = hostOrNull;
        pads = init();
    }

    /**
     * Hardcoded init as the factory default Softstep is starting at CC data1 44
     * and takes up 4 CCs per Pad, each corner (direktion) takes up one.
     */
    public List<Softstep1Pad> init() {
        // Unfortunaltely the internal layout of the softstep is crazy XD, it is like this:
        // [40,48,56,64,72]
        // [44,52,60,68,76]
        // For grid controlling I create the structure manually therefore
        // Note to myself: its upside down because the labeling on the softstep start at the bottom left

        return  new ArrayList<>(Arrays.asList(
                makePad(5,40), makePad(6,48), makePad(7,56), makePad(8,64), makePad(9,72),
                makePad(0,44), makePad(1,52), makePad(2,60), makePad(3,68), makePad(4,76)
        ));
    }

    /**
     * @param startCC a pad has 4 corners on the softstep an the start cc is upper left
     * @return a fresh and shiny pad
     */
    private Softstep1Pad makePad(int number, int startCC) {
        Map<Integer, Integer> tmpDirections = new HashMap<>(4);
        for (int j = 0; j < 4; j++) {
            // Data1 is the key in the directions map,
            // the value is Data2 and initialized with -1
            tmpDirections.put(startCC + j, -1);
        }
        return new Softstep1Pad(number, tmpDirections, host);
    }

    public void update(ShortMidiMessage msg) {
        if (msg.getStatusByte() == SoftstepHardwareBase.STATUS_BYTE) {
            pads.stream().filter(pad -> pad.inRange(msg.getData1()))
                    .findFirst().ifPresent(pad -> {

//                        p("-------------------------");
//                        p("incoming midi: " + msg);
//                        p("matched pad: " + pad);
//                        p("-------------------------");


                        pad.update(msg.getData1(), msg.getData2());



//                        if (pad.getPressure() != msg.getData2()) {
//                            pad.setPressure(msg.getData2());
//                            pad.setHasChanged(true);
//                        }

                    });
        }
    }
}

