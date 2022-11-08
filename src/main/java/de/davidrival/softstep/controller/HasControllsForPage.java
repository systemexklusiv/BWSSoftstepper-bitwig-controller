package de.davidrival.softstep.controller;

import java.util.List;


interface HasControllsForPage {

    Page getPage();

    void processControlls(List<Softstep1Pad> pushedDownPads);

}

