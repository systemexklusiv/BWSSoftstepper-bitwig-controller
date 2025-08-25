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
        // Host mode uses interleaved addressing pattern:
        // Upper row (0,1,2,3,4): 40, 48, 56, 64, 72
        // Lower row (5,6,7,8,9): 44, 52, 60, 68, 76

        return  new ArrayList<>(Arrays.asList(
                makePad(0, new int[]{44, 45, 47, 46}), // Pad 0 (index 0) - lower left → Clip 0
                makePad(1, new int[]{52, 53, 55, 54}), // Pad 1 (index 1) - lower 2nd → Clip 1
                makePad(2, new int[]{60, 61, 63, 62}), // Pad 2 (index 2) - lower 3rd → Clip 2
                makePad(3, new int[]{68, 69, 71, 70}), // Pad 3 (index 3) - lower 4th → Clip 3
                makePad(9, new int[]{76, 77, 79, 78}), // Pad 9 (index 4) - lower right → Bank down
                makePad(5, new int[]{40, 41, 43, 42}), // Pad 5 (index 5) - upper left → Mixer
                makePad(6, new int[]{48, 49, 51, 50}), // Pad 6 (index 6) - upper 2nd → Mixer
                makePad(7, new int[]{56, 57, 59, 58}), // Pad 7 (index 7) - upper 3rd → Bank left
                makePad(8, new int[]{64, 65, 67, 66}), // Pad 8 (index 8) - upper 4th → Bank right
                makePad(4, new int[]{72, 73, 75, 74})  // Pad 4 (index 9) - upper right → Bank up
        ));
    }

    /**
     * @param number pad number
     * @param ccAddresses array of 4 CC addresses for this pad's directions
     * @return a fresh and shiny pad
     */
    private Softstep1Pad makePad(int number, int[] ccAddresses) {
        Map<Integer, Integer> tmpDirections = new HashMap<>(4);
        
        // In host mode, each pad sends pressure values on 4 specific addresses
        // Hardware sends non-consecutive addresses per pad direction
        for (int cc : ccAddresses) {
            tmpDirections.put(cc, -1);
        }
        return new Softstep1Pad(number, tmpDirections, host);
    }

    public void update(ShortMidiMessage msg) {
                        p("-------------------------");
                        p("incoming midi: " + msg);
        if (msg.getStatusByte() == SoftstepHardwareBase.STATUS_BYTE) {
            pads.stream().filter(pad -> pad.inRange(msg.getData1()))
                    .findFirst().ifPresent(pad -> {

                        p("matched pad: " + pad);
                        p("-------------------------");

                        pad.update(msg.getData1(), msg.getData2());

                    });
        }
    }
}

