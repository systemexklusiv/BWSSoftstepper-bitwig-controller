package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import de.davidrival.softstep.api.ApiManager;
import de.davidrival.softstep.api.SimpleConsolePrinter;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class ClipControls extends SimpleConsolePrinter implements HasControllsForPage {


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

    private final Predicate<Softstep1Pad> arePadsForNavigation = (
            pad ->
            pad.getNumber() == Page.PAD_INDICES.NAV_DOWN
            || pad.getNumber() == Page.PAD_INDICES.NAV_LEFT
            || pad.getNumber() == Page.PAD_INDICES.NAV_RIGHT
            || pad.getNumber() == Page.PAD_INDICES.NAV_UP
    );
    private final Predicate<Softstep1Pad> arePadsForChannelstrip = (
            pad ->
                    pad.getNumber() == Page.PAD_INDICES.MUTE_PAD
                            || pad.getNumber() == Page.PAD_INDICES.ARM_PAD
    );

    @Override
    public void processControlls(List<Softstep1Pad> pushedDownPads, ShortMidiMessage msg) {

        if (processNavigationPads(getNavigationPads(pushedDownPads))) return;

        if (processChannelStripPads(getChannelStripPads(pushedDownPads), msg)) return;

        List<Softstep1Pad> padsToConsiderForCLipLaunch = getCLipLaunchPads(pushedDownPads);

        padsToConsiderForCLipLaunch.stream()
                .filter(p -> p.gestures().isLongPress())
                .forEach(pad -> {
                            apiManager.getApiToHost().deleteSlotAt(pad.getNumber());
                            pad.gestures().clearLongPressEvent(); // Clear the long press event
                            pad.notifyControlConsumed();
//                            p("! Delete slot by: " + pad);
                        }
                );
        ///// Single press for clip launch (clean edge detection)
        padsToConsiderForCLipLaunch.stream()
                .filter(p -> p.shouldFireFootOnAction())
                .forEach(pad -> {
                            pad.markAsHasFired();
                            apiManager.getApiToHost().fireSlotAt(pad.getNumber());
                            pad.notifyControlConsumed();
//                            p("! Fire slot by: " + pad);
                        }
                );
    }

    private boolean processNavigationPads(List<Softstep1Pad> padsToConsiderForNavigation) {
        List<Softstep1Pad> navPads = padsToConsiderForNavigation.stream()
                .filter(p -> p.shouldFireFootOnAction())
                .collect(Collectors.toList());

        for (Softstep1Pad p : navPads) {
            switch (p.getNumber()) {
                case Page.PAD_INDICES.NAV_UP:
                    p.markAsHasFired();
                    apiManager.getApiToHost().clipSlotBankUp();
                    p.notifyControlConsumed();
                    return true;
                case Page.PAD_INDICES.NAV_DOWN:
                    p.markAsHasFired();
                    apiManager.getApiToHost().clipSlotBankDown();
                    p.notifyControlConsumed();
                    return true;
                case Page.PAD_INDICES.NAV_LEFT:
                    p.markAsHasFired();
                    apiManager.getApiToHost().clipSlotBankLeft();
                    p.notifyControlConsumed();
                    return true;
                case Page.PAD_INDICES.NAV_RIGHT:
                    p.markAsHasFired();
                    apiManager.getApiToHost().clipSlotBankRight();
                    p.notifyControlConsumed();
                    return true;
            }
        }
        return false;
    }

    private boolean processChannelStripPads(List<Softstep1Pad> padsToConsiderForChannelStrip, ShortMidiMessage msg) {
        // Check for long press actions first
        List<Softstep1Pad> longPressChannelPads = padsToConsiderForChannelStrip.stream()
                .filter(p -> p.gestures().isLongPress())
                .collect(Collectors.toList());
                
        for (Softstep1Pad p : longPressChannelPads) {
            switch (p.getNumber()) {
                case Page.PAD_INDICES.MUTE_PAD:
                    // Long press = stop all clips on current track
                    apiManager.getApiToHost().stopTrack();
                    p.gestures().clearLongPressEvent(); // Clear the long press event
                    p.notifyControlConsumed();
                    return true;
                case Page.PAD_INDICES.ARM_PAD:
                    // Long press = delete all clips on current track
                    apiManager.getApiToHost().deleteAllSlots();
                    p.gestures().clearLongPressEvent(); // Clear the long press event
                    p.notifyControlConsumed();
                    return true;
            }
        }
        
        // Check for short press actions
        List<Softstep1Pad> channelPads = padsToConsiderForChannelStrip.stream()
                .filter(p -> p.shouldFireFootOnAction())
                .collect(Collectors.toList());

            for (Softstep1Pad p : channelPads) {
                switch (p.getNumber()) {
                    case Page.PAD_INDICES.MUTE_PAD:
                        // Simple press = mute/unmute toggle
                        p.markAsHasFired();
                        apiManager.getApiToHost().muteTrack();
                        p.notifyControlConsumed();
                        return true;
                    case Page.PAD_INDICES.ARM_PAD:
                        // Simple press = arm/disarm toggle
                        p.markAsHasFired();
                        apiManager.getApiToHost().armTrack();
                        p.notifyControlConsumed();
                        return true;
                }

        }

        return false;
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


    protected List<Softstep1Pad> getNavigationPads(List<Softstep1Pad> pushedDownPads) {
        return pushedDownPads.stream()
                .filter(arePadsForNavigation)
                .collect(Collectors.toList());
    }

    protected List<Softstep1Pad> getChannelStripPads(List<Softstep1Pad> pushedDownPads) {
        return pushedDownPads.stream()
                .filter(arePadsForChannelstrip)
                .collect(Collectors.toList());
    }
}
