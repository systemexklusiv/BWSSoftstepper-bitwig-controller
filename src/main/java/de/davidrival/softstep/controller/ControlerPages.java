package de.davidrival.softstep.controller;


import java.util.Arrays;
import java.util.Optional;

public class ControlerPages {

    private Page currentPage;

    public ControlerPages(Page startPage) {
        this.currentPage = startPage;
    }

    public void setCurrentPage(Page currentPage) {
        this.currentPage = currentPage;
    }

    public void distributeLedStates(Page page, int padIndex, LedStates ledState ) {
        Optional<Page> pageToSetLedUpdate = Arrays.stream(Page.values())
                .filter(eachPage -> eachPage.equals(page))
                .findFirst();

        pageToSetLedUpdate.ifPresent(
                    p -> {
                        p.ledStates.remove(padIndex);
                        p.ledStates.add(padIndex, ledState);
                    }
                );

    }

    public Page getCurrentPage() {
        return currentPage;
    }



}
