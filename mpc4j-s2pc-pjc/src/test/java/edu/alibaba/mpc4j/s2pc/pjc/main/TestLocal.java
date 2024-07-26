package edu.alibaba.mpc4j.s2pc.pjc.main;

import org.junit.Test;

import java.util.Objects;

public class TestLocal {
    @Test
    public void testMapServer() throws Exception {
        String path = "pmap";
        String configPath = Objects.requireNonNull(TestLocal.class.getClassLoader().getResource(path)).getPath();
        PjcMain4Dir.main(new String[]{configPath, "server"});
    }

    @Test
    public void testMapClient() throws Exception {
        String path = "pmap";
        String configPath = Objects.requireNonNull(TestLocal.class.getClassLoader().getResource(path)).getPath();
        PjcMain4Dir.main(new String[]{configPath, "client"});
    }
}
