package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Sbitmap main class.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public class SbitmapMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(SbitmapMain.class);

    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // read configuration.
        LOGGER.info("read config file");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        SbitmapStarter fullSecureProtocol = new SbitmapStarter(properties);
        // data number

        // 这里应该设置测试不同的数据量
        fullSecureProtocol.init();
        fullSecureProtocol.start();
        System.exit(0);
    }
}
