package edu.alibaba.mpc4j.s2pc.groupagg.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.main.view.PkFkViewMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Group agg main.
 *
 * @author Feng Han
 * @date 2024/7/24
 */
public class GroupAggMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupAggMain.class);

    /**
     * 主函数。
     *
     * @param args 只有一个输入：配置文件。
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // 读取配置文件
        LOGGER.info("read PTO config");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        String ownName = args[1];
        properties.setProperty("own_name", ownName);
        // 读取协议类型
        String ptoType = PropertiesUtils.readString(properties, "pto_type");
        LOGGER.info("pto_type = {}", ptoType);
        switch (ptoType) {
            case PkFkViewMain.PTO_TYPE_NAME:
                PkFkViewMain viewMain = new PkFkViewMain(properties);
                viewMain.runNetty();
                break;
            default:
                throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        System.exit(0);
    }
}
