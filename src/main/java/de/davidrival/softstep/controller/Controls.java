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
        // Host mode uses hardcoded addresses: [44, 52, 60, 68, 76, 40, 48, 56, 64, 72]

        return  new ArrayList<>(Arrays.asList(
                makePad(5,76), makePad(6,40), makePad(7,48), makePad(8,56), makePad(9,64),
                makePad(0,44), makePad(1,52), makePad(2,60), makePad(3,68), makePad(4,72)
        ));
    }

    /**
     * @param baseCC the base CC address for this pad in host mode
     * @return a fresh and shiny pad
     */
    private Softstep1Pad makePad(int number, int baseCC) {
        Map<Integer, Integer> tmpDirections = new HashMap<>(5);
        
        // In host mode, each pad sends pressure values on 4 consecutive addresses
        // Simplified: removed incDec gesture, just 4 directions per pad
        for (int j = 0; j < 4; j++) {
            // Data1 is the key in the directions map,
            // the value is Data2 and initialized with -1
            tmpDirections.put(baseCC + j, -1);
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

