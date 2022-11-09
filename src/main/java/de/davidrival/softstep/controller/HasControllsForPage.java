package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;

import java.util.List;


interface HasControllsForPage {

    Page getPage();

    void processControlls(List<Softstep1Pad> pushedDownPads, ShortMidiMessage msg);

}

