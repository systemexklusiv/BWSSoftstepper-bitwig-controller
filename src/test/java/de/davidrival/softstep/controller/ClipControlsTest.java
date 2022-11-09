package de.davidrival.softstep.controller;

import de.davidrival.softstep.api.ApiManager;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ClipControlsTest {


    private ClipControls clipControlers;

    @Before
    public void init() {
        this.clipControlers = new ClipControls(Page.CLIP, new ApiManager(null));
    }
    @Test
    public void processControlls() {
//        Controls controls = new Controls(null);
//
//        List<Softstep1Pad> pads = controls.getPads();
//        clipControlers.processControlls(pads, msg);
    }



    @Test
    public void getNavigationPads() {
    }
}