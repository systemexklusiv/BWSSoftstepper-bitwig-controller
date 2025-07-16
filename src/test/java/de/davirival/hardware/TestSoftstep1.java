package de.davirival.hardware;

import org.junit.Assert;
import org.junit.Test;

import de.davidrival.softstep.controller.Controls;

import java.util.Optional;
import java.util.stream.Collectors;

public class TestSoftstep1 {

    @Test
    public void isInitWorking() {
        Controls softstep1 = new Controls(null);

        Assert.assertEquals(10, softstep1.getPads().size());
        // Pad at index 0 is makePad(5,76), so CC range is 76-79
        Assert.assertEquals(Optional.of(76).get(), softstep1
                .getPads()
                .get(0)
                .getDirections()
                .keySet()
                .stream()
                .collect(Collectors.toList())
                .get(0));

        // Pad at index 9 is makePad(4,72), so CC range is 72-75, 4th element is 75
        Assert.assertEquals(Optional.of(75).get(), softstep1
                .getPads()
                .get(9)
                .getDirections()
                .keySet()
                .stream()
                .collect(Collectors.toList())
                .get(3));
    }
}
