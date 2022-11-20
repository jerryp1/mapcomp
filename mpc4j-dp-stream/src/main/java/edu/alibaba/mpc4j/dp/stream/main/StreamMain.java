package edu.alibaba.mpc4j.dp.stream.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Stream Main。
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public class StreamMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamMain.class);

    public static void main(String[] args) throws Exception {
        // 读取日志配置文件
        LOGGER.info("read log config");
        Properties log4jProperties = new Properties();
        log4jProperties.load(StreamMain.class.getResourceAsStream("/log4j.properties"));
        PropertyConfigurator.configure(log4jProperties);
        // 读取配置文件
        LOGGER.info("read config file");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        // 读取协议类型
        String taskTypeString = PropertiesUtils.readString(properties, "task_type");
        if (taskTypeString.equals(LdpHeavyHitterMain.TASK_TYPE_NAME)) {
            LdpHeavyHitterMain ldpHeavyHitterMain = new LdpHeavyHitterMain(properties);
            ldpHeavyHitterMain.run();
        } else {
            throw new IllegalArgumentException("Invalid task_type: " + taskTypeString);
        }
        System.exit(0);
    }
}
