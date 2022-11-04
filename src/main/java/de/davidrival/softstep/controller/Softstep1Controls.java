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

    static final int startData1At = 44;
    List<Softstep1Pad> pads = new ArrayList<>(10);

    public Softstep1Controls() {
        init();
    }

    public void init() {
        Softstep1Pad pad;
        int currentData1 = 44;
        for (int i = 0; i < 10; i++) {
            pad = new Softstep1Pad(i);
            for (int j = 0; j < 4; j++) {
                pad.getDirections().put(currentData1 + j, -1);
            }
            pad.init();
            pads.add(pad);
            currentData1 += 4;
        }
    }

    public void update(ShortMidiMessage msg) {
        if ( msg.getStatusByte() == SoftstepHardwareBase.STATUS_BYTE)
        {
            pads.stream().parallel().filter(pad -> pad.inRange(msg.getData1()))
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

