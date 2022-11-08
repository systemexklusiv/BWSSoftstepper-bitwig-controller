package de.davidrival.softstep.controller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class Softstep1PadTest {

    List<Softstep1Pad> pads;
    Softstep1Pad padUnderTest;
    Controls controls;

    @Before
    public void setup() {
        controls = new Controls(null);
        pads = controls.init();
        Map<Integer, Integer> dirs = new HashMap<>(4);
        dirs.put(0,42);
        dirs.put(1,43);
        dirs.put(2,44);
        dirs.put(3,69);
        padUnderTest = new Softstep1Pad(42, dirs, null);
    }


    @Test
    public void init() {
        assertEquals(Optional.of(0).get(), padUnderTest.minData1);
        assertEquals(Optional.of(3).get(), padUnderTest.maxData1);
    }

    @Test
    public void inRange() {
        boolean outcome = padUnderTest.inRange(4);
        assertFalse(outcome);
        outcome = padUnderTest.inRange(-1);
        assertFalse(outcome);
        outcome = padUnderTest.inRange(0);
        assertTrue(outcome);
        outcome = padUnderTest.inRange(3);
        assertTrue(outcome);
    }

    @Test
    public void distributeToDirections() {
        padUnderTest.update(0, 69);
        assertEquals(Optional.of(69).get(), padUnderTest.getDirections().get(0));
        padUnderTest.update(3, 42);
        assertEquals(Optional.of(42).get(), padUnderTest.getDirections().get(3));
    }

    @Test
    public void ensureCrucialMethodsCalled() {
//        Softstep1Pad mockPad = Mockito.spy(padUnderTest);
//        mockPad.update(3, 99);
//        verify(mockPad, times(1)).calcMaxPressureOfDirections(anyMap());
    }

    @Test
    public void calcMaxPressureOfDirectionsTest() {
        int maxPressure = padUnderTest.calcMaxPressureOfDirections(padUnderTest.getDirections());
        assertEquals(69, maxPressure);
        padUnderTest.getDirections().put(0, 777);
        padUnderTest.getDirections().put(1, 1);
        padUnderTest.getDirections().put(2, 7);
        padUnderTest.getDirections().put(3, 99);
        maxPressure = padUnderTest.calcMaxPressureOfDirections(padUnderTest.getDirections());
        assertEquals(777, maxPressure);
    }

    @Test
    public void notifyControlConsumed() {
        padUnderTest.update(0, 69);
        assertTrue(padUnderTest.isUsed());
        padUnderTest.notifyControlConsumed();
        assertFalse(padUnderTest.isUsed());

        padUnderTest.update(0, 69);
        // False because has been sent before
        assertFalse(padUnderTest.isUsed());

        padUnderTest.update(0, 42);
        assertTrue(padUnderTest.isUsed());

        padUnderTest.notifyControlConsumed();
        assertFalse(padUnderTest.isUsed());
    }

    @Test
    public void isUsed() {
    }
}