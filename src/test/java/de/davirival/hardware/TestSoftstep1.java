package de.davirival.hardware;

import de.davidrival.softstep.controller.Softstep1Controls;
import org.junit.Assert;
import org.junit.Test;

public class TestSoftstep1 {

    @Test
    public void isInitWorking() {
        Softstep1Controls softstep1 = new Softstep1Controls();

        Assert.assertEquals(10, softstep1.getPads().size());
        Assert.assertEquals(44, softstep1.getPads().get(0).getDirections().get(0), 44);
        Assert.assertEquals(83, (int) softstep1.getPads().get(9).getDirections().get(3));
    }
}
