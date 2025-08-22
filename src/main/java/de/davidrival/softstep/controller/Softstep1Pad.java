package de.davidrival.softstep.controller;

import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.api.SimpleConsolePrinter;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;


@ToString
public class Softstep1Pad extends SimpleConsolePrinter {

    @Getter
    private final int number;
    /**
     * Each pad of the Softstep has 5 different functions, I call them directions
     * */
    @Getter
    private final Map<Integer, Integer> directions;
    /** The lowest cc data1 of the 4 corners of each pad */
    @Getter
    Integer minData1 = null;
    /** The highest cc data1 of the 4 corners of each pad */
    @Getter
    Integer maxData1 = null;

    @Getter
    /** Flag which tells the Controller class to consider this Pad in triggerering something   */
    private boolean isBeingUsed = false;
    
    /** Flag to prevent multiple firing during a single press */
    private boolean hasAlreadyFiredInThisPress = false;

    @Setter
    private Gestures gestures;

    public Softstep1Pad(int number, Map<Integer, Integer> directions, ControllerHost hostOrNull) {
        super(hostOrNull);
        this.directions = directions;
        this.number = number;
        this.gestures = new Gestures(hostOrNull);
        init();
    }

    public void init() {
        minData1 = directions.entrySet().stream()
                .min(Map.Entry.comparingByKey()).map(Map.Entry::getKey)
                .orElseThrow(NoSuchElementException::new);

        maxData1 = directions.entrySet().stream()
                .max(Map.Entry.comparingByKey()).map(Map.Entry::getKey)
                .orElseThrow(NoSuchElementException::new);
    }


    /**
     * Find out if a given data1 is in range of this pad which ist build up of 4 CCs
     * representing the 4 corners
     * @param data1 checked to be in range
     * @return true if its in the range of this pad
     */
    public boolean inRange(int data1) {
        return data1 >= minData1 && data1 <= maxData1;
    }

    /**
     * Each Softstep Pad has 4 ( I call it so ) directions which allows
     * to find out if one wants to press it left, right, up down for instance
     * Each direction has a separate data1 adress. This method distrbutes the
     * data accordingly.
     *
     * @param data1
     * @param data2
     */
    public void update(int data1, int data2) {
        if (directions.get(data1) != data2) {
            // Set this flag so this control will be considered
            markControlUsed();

            distributeToDirections(data1, data2);

            // Update gesture detection with new pressure data
            gestures.set(this);
        }
    }

    private void distributeToDirections(int data1, int data2) {
        this.directions.put(data1,data2);
    }

    /**
     * The control determins based on user input if its being changed
     * If so this method should be called
     */
    private void markControlUsed() {
        this.isBeingUsed = true;
    }

    /**
     * If this controls action has been used by the application the clients
     * calls this method in order to notify this controller instance to be ready
     * for more user input.
     */
    public void notifyControlConsumed() {
        // Only mark as not being used - don't reset gestures during active press
        this.isBeingUsed = false;
        // Gesture state is managed internally by the state machine
    }



    public int calcMaxPressureOfDirections(Map<Integer, Integer> dirs) {
        return dirs.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getValue)
                .orElseThrow(NoSuchElementException::new);
    }

    public boolean isUsed() {
        return isBeingUsed;
    }

    public Gestures gestures() {
        return gestures;
    }
    
    public boolean shouldFireFootOnAction() {
        return gestures.isFootOn();  // Simple - no more double tracking
    }
    
    public void markAsHasFired() {
        // No longer needed - state machine handles this internally
    }
}

