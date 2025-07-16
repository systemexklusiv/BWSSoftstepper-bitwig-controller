package de.davirival.hardware;

import org.junit.Assert;
import org.junit.Test;

import de.davidrival.softstep.controller.Controls;

import java.util.Optional;
import java.util.stream.Collectors;

public class TestSoftstep1 {

//    @Test
//    public void isInitWorking() {
//        Controls softstep1 = new Controls(null);
//
//        Assert.assertEquals(10, softstep1.getPads().size());
//        // Pad at index 0 is makePad(5, [44,45,47,46]), so first CC should be 44
//        Assert.assertEquals(Optional.of(44).get(), softstep1
//                .getPads()
//                .get(0)
//                .getDirections()
//                .keySet()
//                .stream()
//                .collect(Collectors.toList())
//                .get(0));
//
//        // Pad at index 9 is makePad(4, [72,73,75,74]), so 4th element should be 74
//        Assert.assertEquals(Optional.of(74).get(), softstep1
//                .getPads()
//                .get(9)
//                .getDirections()
//                .keySet()
//                .stream()
//                .collect(Collectors.toList())
//                .get(3));
//    }
}
