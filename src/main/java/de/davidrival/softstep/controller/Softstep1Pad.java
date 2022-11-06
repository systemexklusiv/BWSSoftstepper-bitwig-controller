package de.davidrival.softstep.controller;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@Getter
@Setter
@ToString
public class Softstep1Pad {

    private final int number;
    /**  Each pad of the Softstep has 4 corners, I call them directions
     * fi. pad 1 left upper = 44, right upper = 45, left lower = 46, right lower = 47
     * WARNING: not clockwise, it is going left right - left right
     *
     * Directions saves data2 for each of the corners per press
     *
     * */
    Map<Integer, Integer> directions;
    /** The lowest cc data1 of the 4 corners of each pad */
    Integer minData1 = null;
    /** The highest cc data1 of the 4 corners of each pad */
    Integer maxData1 = null;

    /** pressure saves the max of all directions each time a pad is pressed */
    int pressure = 0;

    /** Flag which tells the Controller class to consider this Pad in triggerering something   */
    public boolean hasChanged;

    public Softstep1Pad(int number, Map<Integer, Integer> directions) {
        this.directions = directions;
        this.number = number;
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

    public void setToDirections(int data1, int data2) {
        if (directions.get(data1) != data2) {

            ////// Set this flag so this control wil be considered
            setHasChanged(true);

            this.directions.put(data1, data2);
            setPressureFromAllDirections();
        }
    }
    private void setPressureFromAllDirections() {
        int maxPressureFromAllDirs = directions.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getValue)
                .orElseThrow(NoSuchElementException::new);

        setAccumulatedPressure(maxPressureFromAllDirs);

    }


    /** pressure saves the last data2 regardless which corner is pressed */
    private void setAccumulatedPressure(int data2) {
        pressure = data2;
    }

}

