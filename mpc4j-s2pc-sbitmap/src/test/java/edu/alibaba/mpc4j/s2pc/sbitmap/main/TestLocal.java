package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import org.junit.Test;

public class TestLocal {
    @Test
    public void testSender() throws Exception {
        GroupAggregationMain.main(new String[]{"/Users/fengdi/Documents/workspace/bitmap/sender_params.txt"});
    }

    @Test
    public void testReceiver() throws Exception {
        GroupAggregationMain.main(new String[]{"/Users/fengdi/Documents/workspace/bitmap/receiver_params.txt"});
    }
}
