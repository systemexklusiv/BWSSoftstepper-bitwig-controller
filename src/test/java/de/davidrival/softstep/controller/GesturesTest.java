package de.davidrival.softstep.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Timer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GesturesTest {

    Gestures gestures;

    @Before
    public void setup(){
        this.gestures = new Gestures(null);
    }
    @Test
    public void checkFootOnThanFootOff() {
        // step down
        boolean footDown = gestures.checkFootOnThanFootOff(Gestures.FOOT_ON_MIN_PRESSURE);
        assertFalse(footDown);
        // lift foot - onAndOff detected
        boolean footUp = gestures.checkFootOnThanFootOff(Gestures.FOOT_ON_MIN_PRESSURE - 1);
        assertTrue(footUp);

        // Foot down again
        footDown = gestures.checkFootOnThanFootOff(Gestures.FOOT_ON_MIN_PRESSURE);
        assertFalse(footDown);

        // Maybe hardware error another foot on in succession must yield false again
        footDown = gestures.checkFootOnThanFootOff(Gestures.FOOT_ON_MIN_PRESSURE);
        assertFalse(footDown);

        // This should not make it a FootOnOff recogonition
        // FootOnAndThanOff is only when pushed and relesed again. Tehcnically is foot off if
        // if pressure lower than Gestures.FOOT_ON_MIN_PRESSURE
        boolean footDown2ndTime = gestures.checkFootOnThanFootOff(Gestures.FOOT_ON_MIN_PRESSURE);
        assertFalse(footDown2ndTime);

    }

    @Test
    public void checkLongPress_startingPress() {
        GestureTimer mockGestureTimer = mock(GestureTimer.class);
        gestures.setGestureTimer(mockGestureTimer);

        when(mockGestureTimer.go()).thenReturn(true);

        assertNull(mockGestureTimer.getTimer());

        boolean outcome = gestures.checkLongPress(Gestures.FOOT_ON_MIN_PRESSURE);

        verify(mockGestureTimer, times(1)).isRunning();
        verify(mockGestureTimer, times(1)).start();
        verify(mockGestureTimer, never()).stop();
        assertFalse(outcome);
//        another subsequent won't change unless time has passed
        outcome = gestures.checkLongPress(Gestures.FOOT_ON_MIN_PRESSURE);
        assertFalse(outcome);
    }

    @Test
    public void checkLongPress_footOn_full_time() {
        GestureTimer mockGestureTimer = mock(GestureTimer.class);


        Timer mockJavaTimer = mock(Timer.class);
        mockGestureTimer.setTimer(mockJavaTimer);

        gestures.setGestureTimer(mockGestureTimer);
        // below the long press time..
        when(mockGestureTimer.isRunning()).thenReturn(true);

        when(mockGestureTimer.getDeltaTime()).thenReturn(Gestures.LONG_PRESS_TIME);

        boolean outcome = gestures.checkLongPress(Gestures.FOOT_ON_MIN_PRESSURE);

        verify(mockGestureTimer, times(1)).isRunning();
        verify(mockGestureTimer, never()).start();
        verify(mockGestureTimer, times(1)).stop();

//        verify(mockJavaTimer, times(1)).cancel();

        assertTrue(outcome);
    }

    @Test
    public void checkLongPress_footOn_too_short() {

        GestureTimer mockGestureTimer = mock(GestureTimer.class);

        Timer mockJavaTimer = mock(Timer.class);
        mockGestureTimer.setTimer(mockJavaTimer);

        gestures.setGestureTimer(mockGestureTimer);
        // below the long press time..
        when(mockGestureTimer.isRunning()).thenReturn(true);
        when(mockGestureTimer.getDeltaTime()).thenReturn(Gestures.LONG_PRESS_TIME-1);

        boolean outcome = gestures.checkLongPress(Gestures.FOOT_ON_MIN_PRESSURE);

        verify(mockGestureTimer, times(1)).isRunning();
        verify(mockGestureTimer, never()).start();
        verify(mockGestureTimer, never()).stop();
        assertFalse(outcome);
    }

    @Test
    public void checkLongPress_no_measuring_if_not_pressed_enough() {
        GestureTimer mockGestureTimer = mock(GestureTimer.class);
        gestures.setGestureTimer(mockGestureTimer);

        when(mockGestureTimer.getDeltaTime()).thenReturn(Gestures.LONG_PRESS_TIME);
        when(mockGestureTimer.isRunning()).thenReturn(true);

        boolean outcome = gestures.checkLongPress(Gestures.FOOT_ON_MIN_PRESSURE-1);

        verify(mockGestureTimer, never()).isRunning();
        verify(mockGestureTimer, never()).start();
        verify(mockGestureTimer, never()).getDeltaTime();
        // stopping the measurement and all if the foot is liftet up
        verify(mockGestureTimer, times(1)).stop();
        assertFalse(outcome);
    }
}