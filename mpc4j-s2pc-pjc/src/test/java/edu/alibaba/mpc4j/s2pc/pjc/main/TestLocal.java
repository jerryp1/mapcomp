package edu.alibaba.mpc4j.s2pc.pjc.main;

import org.junit.Test;

public class TestLocal {
    @Test
    public void testMapServer() throws Exception {
        PjcMain4Dir.main(new String[]{"/Users/fengdi/Documents/workspace/mpc4j/mpc4j-s2pc-pjc/src/test/resources/exp", "server"});
    }

    @Test
    public void testMapClient() throws Exception {
        PjcMain4Dir.main(new String[]{"/Users/fengdi/Documents/workspace/mpc4j/mpc4j-s2pc-pjc/src/test/resources/exp", "client"});
    }
}
