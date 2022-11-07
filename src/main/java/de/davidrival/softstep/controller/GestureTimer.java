package de.davidrival.softstep.controller;

import lombok.Getter;
import lombok.Setter;

import java.util.Timer;
import java.util.TimerTask;

@Getter
@Setter
public class GestureTimer {

    protected Timer timer;
    private int deltaTime = 0;

    public void start() {
        timer = new Timer();
        deltaTime = 0;
        go();
    }

    protected boolean go() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                deltaTime += 1;
            }
        }, 0, 1);
        return true;
    }

    public void stop() {
        timer.cancel();
        timer = null;
    }

    public boolean isRunning() {
        return timer != null
                && deltaTime > 0;
    }

}
