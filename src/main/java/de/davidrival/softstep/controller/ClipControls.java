package de.davidrival.softstep.controller;

import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.SimpleConsolePrinter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class ClipControls extends SimpleConsolePrinter implements HasControllsForPage {

    public static final int PAD_NUM_UP = 9;
    public static final int PAD_NUM_DOWN = 4;
    public static final int PAD_NUM_LEFT = 7;
    public static final int PAGE_NUM_RIGHT = 8;

    public ClipControls(Page page, ApiManager apiManager) {
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


    private final Predicate<Softstep1Pad> arePadsForNavigation = (pad -> pad.getNumber() == PAD_NUM_DOWN
                                                            || pad.getNumber() == PAD_NUM_LEFT
                                                            || pad.getNumber() == PAGE_NUM_RIGHT
                                                            || pad.getNumber() == PAD_NUM_UP
    );

    /** Keeps track of the Pad index which has been pressed long in order not to get confused by
     * the usual pressed ones */
    final AtomicInteger data1OfLongPressedPad = new AtomicInteger(-1);

    @Override
    public void processControlls(List<Softstep1Pad> pushedDownPads) {

        List<Softstep1Pad> padsToConsiderForNavigation = getNavigationPads(pushedDownPads);

        if (processNavigationPads(padsToConsiderForNavigation)) return;


        List<Softstep1Pad> padsToConsiderForCLipLaunch = getCLipLaunchPads(pushedDownPads);

        //// LONG PRESS STUFF
                ///// First Check long press
                // only if the pressure is almost 0 a prior long pressed pad is given free for
                // other clip launching tasks
                padsToConsiderForCLipLaunch.stream()
                        .filter(p -> p.getNumber() == data1OfLongPressedPad.get())
                                .forEach(p -> {
                                    if (p.getPressure() < 2) {
                                        data1OfLongPressedPad.set(-1);
                                    }
                                });


                padsToConsiderForCLipLaunch.stream()
                        .filter(p -> p.gestures().isLongPress())
                        .forEach(pad -> {
                                    if (pad.gestures().isLongPress()) {
                                        apiManager.deleteSlotAt(pad.getNumber());

                                        data1OfLongPressedPad.set(pad.getNumber());

                                        pad.notifyControlConsumed();

                                        p("! Delete slot by: " + pad);

                                        return;
                                    }

                                }
                        );
                ///// Foot Ons for clip launch
                padsToConsiderForCLipLaunch.stream()
                            .filter(p -> p.gestures().isFootOn())
                            .forEach(pad -> {
                                        if (!(data1OfLongPressedPad.get() == pad.getNumber())) {
                                            apiManager.fireSlotAt(pad.getNumber());

                                            pad.notifyControlConsumed();

                                            data1OfLongPressedPad.set(-1);

                                            p("! Fire slot by: " + pad);

                                            return;
                                        } else {
                                            p("skipping pad which was longpress: " + pad);
                                        }
                                    }
                            );
    }

    private List<Softstep1Pad> getCLipLaunchPads(List<Softstep1Pad> pushedDownPads) {
        List<Softstep1Pad> padsToConsiderForCLipLaunch = pushedDownPads.stream()
                // In case of firing up clips they must not pads with higher
                // indexes as there are scenes or bitwig will complain and shutdown
                // If done this way implies the layout, clip pads are from 1 to 4
                .filter(pad -> pad.getNumber() < ApiManager.NUM_SCENES)
                .collect(Collectors.toList());
        return padsToConsiderForCLipLaunch;
    }

    private boolean processNavigationPads(List<Softstep1Pad> padsToConsiderForNavigation) {
        List<Softstep1Pad> navPads = padsToConsiderForNavigation.stream()
                .filter(p -> p.gestures().isFootOnThanFootOff()).collect(Collectors.toList());

        for (Softstep1Pad p :  navPads) {
            switch (p.getNumber()){
                case PAD_NUM_UP:
                    apiManager.clipSlotBankUp();
                    return true;
                case PAD_NUM_DOWN:
                    apiManager.clipSlotBankDown();
                    return true;
                case PAD_NUM_LEFT:
                    apiManager.clipSlotBankLeft();
                    return true;
                case PAGE_NUM_RIGHT:
                    apiManager.clipSlotBankRight();
                    return true;
            }
        }
        return false;
    }

    protected List<Softstep1Pad> getNavigationPads(List<Softstep1Pad> pushedDownPads) {
        List<Softstep1Pad> padsToConsiderForNavigation = pushedDownPads.stream()
        .filter(arePadsForNavigation)
        .filter(p -> p.gestures().isFootOnThanFootOff())
        .collect(Collectors.toList());
        return padsToConsiderForNavigation;
    }
}
