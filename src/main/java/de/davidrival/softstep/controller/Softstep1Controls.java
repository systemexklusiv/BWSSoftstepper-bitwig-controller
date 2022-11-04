package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import de.davidrival.softstep.hardware.SoftstepHardwareBase;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class Softstep1Controls {

    static final int START_DATA_1_AT = 44;
    List<Softstep1Pad> pads;

    public Softstep1Controls() {
        pads = init();
    }

    /**
     * Hardcoded init as the factory default Softstep is starting att CC data1 44
     * and takes up 4 CCs per Pad, each corner (direktion) takes up one.
     */
    public List<Softstep1Pad> init() {
        Softstep1Pad pad;
        List<Softstep1Pad> tmpPads = new ArrayList<>(10);
        int currentData1 = 44;
        for (int i = 0; i < 10; i++) {
            pad = new Softstep1Pad(i);
            for (int j = 0; j < 4; j++) {
                pad.getDirections().put(currentData1 + j, -1);
            }
            pad.init();
            tmpPads.add(pad);
            currentData1 += 4;
        }
        return tmpPads;
    }

    public void update(ShortMidiMessage msg) {
        if ( msg.getStatusByte() == SoftstepHardwareBase.STATUS_BYTE)
        {
            pads.stream().filter(pad -> pad.inRange(msg.getData1()))
                    .findFirst().ifPresent(pad -> {
                        if (pad.getPressure() != msg.getData2()) {
                            pad.setPressure(msg.getData2());
                            pad.setHasChanged(true);
                        }

                        if(pad.directions.get(msg.getData1()) != msg.getData2()) {
                            pad.directions.put(msg.getData1(), msg.getData2());
                        }
                    });
        }
    }
}

