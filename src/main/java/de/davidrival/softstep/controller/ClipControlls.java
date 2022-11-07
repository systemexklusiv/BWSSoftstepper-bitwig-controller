package de.davidrival.softstep.controller;

import com.bitwig.extension.controller.api.ControllerHost;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.SimpleConsolePrinter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class ClipControlls extends SimpleConsolePrinter implements HasControllsForPage {

    public ClipControlls(Page page, ApiManager apiManager) {
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

    /** Keeps track of the Pad index which has been pressed long in order not to get confused by
     * the usual pressed ones */
    private final AtomicInteger data1OfLongPressedPad = new AtomicInteger(-1);

    @Override
    public void processControlls(List<Softstep1Pad> pushedDownPads) {
                        List<Softstep1Pad> padsToConsiderForCLipLaunch = pushedDownPads.stream()
                        // In case of firing up clips their must not pads with higher
                        // indexes as there are scenes or bitwig will complain and shutdown
                        .filter(pad -> pad.getNumber() < ApiManager.NUM_SCENES)
                        .collect(Collectors.toList());
                //// LONG PRESS STUFF
                ///// First Check long press
                padsToConsiderForCLipLaunch.stream()
                        .filter(p -> p.gestures().isLongPress())
                        .forEach(pad -> {
                                    if (pad.gestures().isLongPress()) {
                                        apiManager
                                                .getSlotBank()
                                                .getItemAt(pad.getNumber())
                                                .deleteObject();

                                        data1OfLongPressedPad.set(pad.getNumber());

                                        pad.notifyControlConsumed();
                                        p("! Delete slot by: " + pad);
                                        return;
                                    }

                                }
                        );
                ///// Foot On Offs for clip launch
                padsToConsiderForCLipLaunch.stream()
                            .filter(p -> p.gestures().isFootOn())
                            .forEach(pad -> {
                                        if (!(data1OfLongPressedPad.get() == pad.getNumber())) {
                                            p("! Fire slot by: " + pad);
                                            apiManager.fireSlotAt(pad.getNumber());
                                            pad.notifyControlConsumed();
                                            data1OfLongPressedPad.set(-1);
                                            return;
                                        } else {
                                            p("skipping pad which was longpress: " + pad);
                                        }
                                    }
                            );
    }
}
