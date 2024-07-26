package edu.alibaba.mpc4j.s2pc.groupagg.main;

import org.junit.Test;

import java.util.Objects;

public class TestLocal {
    @Test
    public void testViewServer() throws Exception {
        String path = "view";
        String configPath = Objects.requireNonNull(TestLocal.class.getClassLoader().getResource(path)).getPath();
        GroupAggMainBatch.main(new String[]{configPath, "server"});
    }

    @Test
    public void testViewClient() throws Exception {
        String path = "view";
        String configPath = Objects.requireNonNull(TestLocal.class.getClassLoader().getResource(path)).getPath();
        GroupAggMainBatch.main(new String[]{configPath, "client"});
    }

}
