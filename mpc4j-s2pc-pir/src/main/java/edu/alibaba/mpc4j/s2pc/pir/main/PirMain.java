package edu.alibaba.mpc4j.s2pc.pir.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.main.batchindex.BatchIndexPirMain;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * PIR主函数。
 *
 * @author Liqiang Peng
 * @date 2023/3/20
 */
public class PirMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PirMain.class);

    /**
     * 主函数。
     *
     * @param args 只有一个输入：配置文件。
     */
    public static void main(String[] args) throws Exception {
        // 读取日志配置文件
        LOGGER.info("read log config");
        Properties log4jProperties = new Properties();
        log4jProperties.load(PirMain.class.getResourceAsStream("/log4j.properties"));
        PropertyConfigurator.configure(log4jProperties);
        // 读取配置文件
        LOGGER.info("read PTO config");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        // 读取协议类型
        String ptoType = PropertiesUtils.readString(properties, "pto_type");
        LOGGER.info("pto_type = " + ptoType);
        if (BatchIndexPirMain.PTO_TYPE_NAME.equals(ptoType)) {
            BatchIndexPirMain main = new BatchIndexPirMain(properties);
            main.run();
        } else {
            throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        System.exit(0);
    }
}
