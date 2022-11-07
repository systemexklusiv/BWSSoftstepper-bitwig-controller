package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.ControllerHost;

import java.util.Optional;

public class SimpleConsolePrinter {

    private Optional<ControllerHost> host;

    public SimpleConsolePrinter(ControllerHost host) {
        this.host = Optional.ofNullable(host);
    }

    public SimpleConsolePrinter() {
    }

    public void p(String text) {
        host.ifPresentOrElse(h -> h.println(text),
                System.out::println);
    }

    public void e(String text) {
        host.ifPresentOrElse(h -> h.errorln(text),
                System.err::println);
    }

    public void setHost(ControllerHost host) {
        this.host = Optional.ofNullable(host);
    }

    public ControllerHost getHost() {
            return host.orElseGet(null);
    }
}
