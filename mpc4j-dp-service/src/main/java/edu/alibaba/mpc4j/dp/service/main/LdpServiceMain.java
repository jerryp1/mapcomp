package edu.alibaba.mpc4j.dp.service.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * LDP service main class.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public class LdpServiceMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdpServiceMain.class);

    public static void main(String[] args) throws Exception {
        // read log config file
        LOGGER.info("read log config");
        Properties log4jProperties = new Properties();
        log4jProperties.load(new FileInputStream("log4j.properties"));
        PropertyConfigurator.configure(log4jProperties);
        // read config file
        LOGGER.info("read config file");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        // 读取协议类型
        String taskTypeString = PropertiesUtils.readString(properties, "task_type");
        if (taskTypeString.equals(HhLdpMain.TASK_TYPE_NAME)) {
            HhLdpMain hhLdpMain = new HhLdpMain(properties);
            hhLdpMain.run();
        } else {
            throw new IllegalArgumentException("Invalid task_type: " + taskTypeString);
        }
        System.exit(0);
    }
}
