package de.davirival.hardware;

import de.davidrival.softstep.hardware.standalone.Softstep1Standalone;
import org.junit.Assert;
import org.junit.Test;

public class TestSoftstep1 {

    @Test
    public void isInitWorking() {
        Softstep1Standalone softstep1 = new Softstep1Standalone();

        Assert.assertEquals(10, softstep1.getPads().size());
        Assert.assertEquals(44, softstep1.getPads().get(0).getDirections().get(0), 44);
        Assert.assertEquals(83, (int) softstep1.getPads().get(9).getDirections().get(3));
    }
}
