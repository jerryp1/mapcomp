package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.plpsi.PlpsiMain4Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PsoMain4Dir {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain4Dir.class);
    /**
     * warmup element byte length
     */
    private static final int WARMUP_ELEMENT_BYTE_LENGTH = 16;
    /**
     * warmup set size
     */
    private static final int WARMUP_SET_SIZE = 1 << 10;

    /**
     * 主函数。
     *
     * @param args 第一个输入是配置文件所在的目录
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        File folder = new File(args[0]);
        File[] files = folder.listFiles();
        List<String> allFileName = new LinkedList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() & file.getName().endsWith(".txt")) {
                    LOGGER.info(file.getName());
                    allFileName.add(file.getPath());
                }
            }
        }
        Collections.sort(allFileName);
        allFileName.forEach(LOGGER::info);
        Rpc ownRpc = null;
        for(String fileName : allFileName){
            // read config
            LOGGER.info("read PTO config:{}", fileName);
            Properties properties = PropertiesUtils.loadProperties(fileName);
            // 读取协议类型
            String ptoType = PropertiesUtils.readString(properties, "pto_type");
            LOGGER.info("pto_type = " + ptoType);
            properties.setProperty("own_name", args[1]);
            if(args[1].equals("server")){
                generateInputFiles(properties);
            }
            if(ownRpc == null){
                //直接在这里初始化并连接netty
                ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "server", "client");
                ownRpc.connect();
            }
            switch (ptoType) {
                case PlpsiMain4Batch.PTO_TYPE_NAME:
                    PlpsiMain4Batch plpsiMain4Batch = new PlpsiMain4Batch(properties);
                    plpsiMain4Batch.runNetty(ownRpc);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
            }
        }
        System.exit(0);
    }


    public static void generateInputFiles(Properties properties) throws IOException {
        // 读取协议参数
        LOGGER.info("read settings");
        // 读取元素字节长度
        int elementByteLength = PropertiesUtils.readInt(properties, "element_byte_length");
        // 读取集合大小
        int[] logSetSizes = PropertiesUtils.readLogIntArray(properties, "log_set_size");
        int setSizeNum = logSetSizes.length;
        int[] serverSetSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] clientSetSizes = Arrays.stream(logSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 生成输入文件
        LOGGER.info("generate warm-up element files");
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("generate element files");
        for (int setSizeIndex = 0 ; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        LOGGER.info("create result file");
    }
}
