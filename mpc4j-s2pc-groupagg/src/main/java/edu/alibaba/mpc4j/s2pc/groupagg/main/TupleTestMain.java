package edu.alibaba.mpc4j.s2pc.groupagg.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Triple test main class
 *
 */
public class TupleTestMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(TupleTestMain.class);

    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // read configuration.
        LOGGER.info("read config file");
        int num = Integer.parseInt(args[0]);
        Properties properties = PropertiesUtils.loadProperties(args[1]);
        TupleTestStarter protocol = new TupleTestStarter(num, properties);
        protocol.init();
        protocol.start();
        System.exit(0);
    }
}
