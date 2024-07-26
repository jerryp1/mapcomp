package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.s2pc.pso.main.plpsi.MainPlpsiTest;
import org.junit.Test;

import java.util.Objects;

public class MainPso4DirTest {
    @Test
    public void testServer() throws Exception {
        String path = "plpsi";
        String configPath = Objects.requireNonNull(MainPlpsiTest.class.getClassLoader().getResource(path)).getPath();
        PsoMain4Dir.main(new String[]{configPath, "server"});
    }

    @Test
    public void testClient() throws Exception {
        String path = "plpsi";
        String configPath = Objects.requireNonNull(MainPlpsiTest.class.getClassLoader().getResource(path)).getPath();
        PsoMain4Dir.main(new String[]{configPath, "client"});
    }
}
