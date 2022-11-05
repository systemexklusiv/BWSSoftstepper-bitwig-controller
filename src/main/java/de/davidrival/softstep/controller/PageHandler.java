package de.davidrival.softstep.controller;

import de.davidrival.softstep.hardware.LedColor;
import de.davidrival.softstep.hardware.LedLight;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PageHandler {

    // TODO setze alle LED auch wenn die Seite nocht sichtbar ist anhand der API Callbakcs
    // dann wird beim wechsel der PAGES immer der richtige Status angezeigt
    private PageHandler(){}

    private static Pages currentPage;
    public static ArrayList<LedStates> ledStateBuffer;


    public static void setCurrentPage(Pages currentPage) {
        ledStateBuffer = new ArrayList<>(currentPage.initialLedStates);
        PageHandler.currentPage = currentPage;
    }

    public static Pages getCurrentPage() {
        return currentPage;
    }

    public static ArrayList<LedStates> getLedStateBuffer() {
        return ledStateBuffer;
    }

    public static void setCurrentState(int index, LedStates ledStates) {
        ledStateBuffer.remove(index);
        ledStateBuffer.add(index, ledStates);
    }
}
