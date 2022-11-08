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
public class Controls extends SimpleConsolePrinter {

    List<Softstep1Pad> pads;

    ControllerHost host;

    public Controls(ControllerHost hostOrNull) {
        super(hostOrNull);
        this.host = hostOrNull;
        pads = init();
    }


    public List<Softstep1Pad> init() {
        // Note to myself: its upside down because the labeling on the softstep start at the bottom left

        return  new ArrayList<>(Arrays.asList(
                makePad(5,25), makePad(6,30), makePad(7,35), makePad(8,40), makePad(9,45),
                makePad(0,0), makePad(1,5), makePad(2,10), makePad(3,15), makePad(4,20)
        ));
    }

    /**
     * @param startCC a pad has 4 corners on the softstep an the start cc is upper left
     * @return a fresh and shiny pad
     */
    private Softstep1Pad makePad(int number, int startCC) {
        Map<Integer, Integer> tmpDirections = new HashMap<>(5);
        for (int j = 0; j < 5; j++) {
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

