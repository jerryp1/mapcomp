package edu.alibaba.mpc4j.s2pc.groupagg.main;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.groupagg.main.view.PkFkViewMain;
import edu.alibaba.mpc4j.s2pc.groupagg.main.view.PkFkViewMainBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Group agg main batch
 *
 */
public class GroupAggMainBatch {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupAggMainBatch.class);

    /**
     * 主函数。
     *
     * @param args 只有一个输入：配置文件。
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // 读取配置文件
        LOGGER.info("read PTO config");

        File dir = new File(args[0]);
        String ownName = args[1];
        File[] fs = dir.listFiles();
        List<String> allFiles = new LinkedList<>();
        for(File f : fs){
            if((!f.isDirectory()) && f.getPath().endsWith(".config")){
                allFiles.add(f.getPath());
            }
        }
        String[] names = allFiles.stream().sorted().toArray(String[]::new);
        Properties properties = PropertiesUtils.loadProperties(names[0]);

        properties.setProperty("own_name", ownName);
        Rpc ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "server", "client");
        ownRpc.connect();
        // 读取协议类型
        String ptoType = PropertiesUtils.readString(properties, "pto_type");
        LOGGER.info("pto_type = " + ptoType);
        switch (ptoType) {
            case PkFkViewMain.PTO_TYPE_NAME:
                for(String filePath : names){
                    properties = PropertiesUtils.loadProperties(filePath);
                    PkFkViewMainBatch viewMain = new PkFkViewMainBatch(properties);
                    viewMain.runNetty(ownRpc);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        ownRpc.disconnect();
        System.exit(0);
    }
}
