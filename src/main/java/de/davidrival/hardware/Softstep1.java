package de.davidrival.hardware;

import de.davidrival.hardware.standalone.Softstep1PadStandalone;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class Softstep1 {
    public static final int CHANNEL = 0;
    List<Integer> pads = new ArrayList<>(10);

    public Softstep1() {
        init();
    }

    public void init() {
        for (int i = 0; i < 10; i++) {
            pads.add(i);
        }
    }
}


