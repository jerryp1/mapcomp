package edu.alibaba.mpc4j.s2pc.pso.main;

import org.junit.Test;

public class MainPso4DirTest {
    @Test
    public void runServer() throws Exception {
        PsoMain4Dir.main(new String[]{"/Users/fengdi/Documents/workspace/mpc4j/mpc4j-s2pc-pso/src/test/resources/testra17", "server"});
    }
    @Test
    public void runClient() throws Exception {
        PsoMain4Dir.main(new String[]{"/Users/fengdi/Documents/workspace/mpc4j/mpc4j-s2pc-pso/src/test/resources/testra17", "client"});
    }

}
