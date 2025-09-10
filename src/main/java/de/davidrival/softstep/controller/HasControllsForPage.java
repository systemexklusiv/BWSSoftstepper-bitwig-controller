package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;

import java.util.List;


interface HasControllsForPage {

    Page getPage();

    void processControlls(List<Softstep1Pad> pushedDownPads, ShortMidiMessage msg);
    
    /**
     * Refreshes LED states from current clip/control states.
     * Called when switching to this mode to ensure LEDs reflect current state.
     */
    void refreshLedStates();

}

