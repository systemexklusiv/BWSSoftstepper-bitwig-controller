package de.davidrival.hardware.standalone;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Softstep1PadStandalone {
    List<Integer> directions = new ArrayList<>(4);
}
