package de.davirival.hardware;

import de.davidrival.softstep.controller.Controlls;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.stream.Collectors;

public class TestSoftstep1 {

    @Test
    public void isInitWorking() {
        Controlls softstep1 = new Controlls(null);

        Assert.assertEquals(10, softstep1.getPads().size());
        Assert.assertEquals(Optional.of(40).get(), softstep1
                .getPads()
                .get(0)
                .getDirections()
                .keySet()
                .stream()
                .collect(Collectors.toList())
                .get(0));

        Assert.assertEquals(Optional.of(79).get(), softstep1
                .getPads()
                .get(9)
                .getDirections()
                .keySet()
                .stream()
                .collect(Collectors.toList())
                .get(3));
    }
}
