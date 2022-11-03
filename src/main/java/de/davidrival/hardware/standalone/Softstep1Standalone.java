package de.davidrival.hardware.standalone;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class Softstep1Standalone {
    static final int status = 176;
    static final int startData1At = 44;
    List<Softstep1PadStandalone> pads = new ArrayList<>(10);

    public Softstep1Standalone() {
        init();
    }

    public void init() {
        Softstep1PadStandalone pad;
        int currentData1 = 44;
        for (int i = 0; i < 10; i++) {
            pad = new Softstep1PadStandalone();
            for (int j = 0; j < 4; j++) {
                pad.getDirections().add(currentData1 + j);
            }
            pads.add(pad);
            currentData1 += 4;
        }
    }

}

