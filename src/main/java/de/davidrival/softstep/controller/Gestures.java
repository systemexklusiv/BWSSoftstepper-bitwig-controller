package de.davidrival.softstep.controller;

import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.api.SimpleConsolePrinter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Gestures extends SimpleConsolePrinter {

    public boolean isFootOnThanFootOff = false;

    public boolean isLongPress = false;

    public Gestures(ControllerHost hostOrNull) {
        super(hostOrNull);
    }
}
