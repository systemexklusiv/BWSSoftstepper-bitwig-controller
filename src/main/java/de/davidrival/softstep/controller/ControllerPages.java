package de.davidrival.softstep.controller;


import java.util.Arrays;
import java.util.Optional;

public class ControllerPages {

    // TODO setze alle LED auch wenn die Seite nocht sichtbar ist anhand der API Callbakcs
    // dann wird beim wechsel der PAGES immer der richtige Status angezeigt
    private Page currentPage;

    public ControllerPages(Page startPage) {
        this.currentPage = startPage;
    }

    public void setCurrentPage(Page currentPage) {
        this.currentPage = currentPage;
    }

    public void distributeLedStates(Page page, int padIndex, LedStates ledStates ) {
        Optional<Page> pageToSetLedUpdate = Arrays.stream(Page.values())
                .filter(eachPage -> eachPage.equals(page))
                .findFirst();

        pageToSetLedUpdate.ifPresent(
                    p -> {
                        p.ledStates.remove(padIndex);
                        p.ledStates.add(padIndex, ledStates);
                    }
                );

    }

    public Page getCurrentPage() {
        return currentPage;
    }



}
