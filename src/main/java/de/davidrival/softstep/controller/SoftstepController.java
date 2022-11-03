package de.davidrival.softstep.controller;

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
        showLeds();

    }
    public void showLeds() {
       softstepHardware.showLeds(currentPage);
    }
}
