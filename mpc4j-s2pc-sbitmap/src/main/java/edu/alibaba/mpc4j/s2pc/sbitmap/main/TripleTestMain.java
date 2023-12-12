package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Triple test main class
 *
 * @author Li Peng
 * @date 2023/11/24
 */
public class TripleTestMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripleTestMain.class);

    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // read configuration.
        LOGGER.info("read config file");
        int num = Integer.parseInt(args[0]);
        String type = args[1];
        Properties properties = PropertiesUtils.loadProperties(args[2]);
        TripleTestStarter protocol = new TripleTestStarter(num, type, properties);
        // data number

        protocol.init();
        protocol.start();
        System.exit(0);
    }
}
