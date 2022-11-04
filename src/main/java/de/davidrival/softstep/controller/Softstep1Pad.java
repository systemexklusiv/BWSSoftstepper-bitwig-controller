package de.davidrival.softstep.controller;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class Softstep1Pad {

    private final int number;
    // Data 1 und 2
    Map<Integer, Integer> directions = new HashMap<>(4);

    Integer min = null;
    Integer max = null;

    int pressure = 0;
    public boolean hasChanged;

    public Softstep1Pad(int number) {
        this.number = number;
    }

    public boolean inRange(int data1) {
        return data1 >= min && data1 <= max;
    }

    public void init() {
        if (min == null) {
            min = directions.entrySet().stream()
                    .min(Map.Entry.comparingByKey()).map(Map.Entry::getKey)
                    .orElseThrow(NoSuchElementException::new);
        }
        if (max == null) {
            max = directions.entrySet().stream()
                    .max(Map.Entry.comparingByKey()).map(Map.Entry::getKey)
                    .orElseThrow(NoSuchElementException::new);
        }
    }

    public void setPressure(int data2) {
        pressure = data2;
    }

}
