package edu.alibaba.mpc4j.s2pc.groupagg.main;

import org.junit.Test;

public class TestLocal {
    @Test
    public void testServer() throws Exception {
        OneSideGroupMain.main(new String[]{"/Users/fengdi/Documents/workspace/mpc4j/mpc4j-s2pc-aby/src/test/resources/group/conf_one_side_group.txt", "server"});
    }

    @Test
    public void testClient() throws Exception {
        OneSideGroupMain.main(new String[]{"/Users/fengdi/Documents/workspace/mpc4j/mpc4j-s2pc-aby/src/test/resources/group/conf_one_side_group.txt", "client"});
    }
}
