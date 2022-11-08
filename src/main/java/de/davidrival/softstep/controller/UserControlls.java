package de.davidrival.softstep.controller;

import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.SimpleConsolePrinter;

import java.util.List;


public class UserControlls extends SimpleConsolePrinter implements HasControllsForPage {

    public UserControlls(Page page, ApiManager apiManager) {
        super(apiManager.getHost());
        this.page = page;
        this.apiManager = apiManager;
    }

    private final Page page;
    private final ApiManager apiManager;

    @Override
    public Page getPage() {
        return this.page;
    }

    @Override
    public void processControlls(List<Softstep1Pad> pushedDownPads) {
        pushedDownPads.forEach(pad -> {
            apiManager.setValueOfUserControl(pad.getNumber(), pad.gestures().getPressure());
            pad.notifyControlConsumed();
        }  );
    }
}
