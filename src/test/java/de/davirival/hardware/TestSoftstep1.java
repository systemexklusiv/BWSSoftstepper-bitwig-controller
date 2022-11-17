package de.davirival.hardware;

import de.davidrival.softstep.controller.Controls;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.stream.Collectors;

public class TestSoftstep1 {

    @Test
    public void isInitWorking() {
        Controls softstep1 = new Controls(null);

        Assert.assertEquals(10, softstep1.getPads().size());
        Assert.assertEquals(Optional.of(25).get(), softstep1
                .getPads()
                .get(0)
                .getDirections()
                .keySet()
                .stream()
                .collect(Collectors.toList())
                .get(0));

        Assert.assertEquals(Optional.of(22).get(), softstep1
                .getPads()
                .get(9)
                .getDirections()
                .keySet()
                .stream()
                .collect(Collectors.toList())
                .get(3));
    }
}
