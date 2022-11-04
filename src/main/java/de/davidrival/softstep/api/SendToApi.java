package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.UserControlBank;

public class SendToApi {

    public UserControlBank userControls;

    public SendToApi(ControllerHost host) {

         userControls = host.createUserControls(10);

    }
}
