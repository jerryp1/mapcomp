package edu.alibaba.mpc4j.s2pc.pso.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.psi.PsiMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuBlackIpMain;
import edu.alibaba.mpc4j.s2pc.pso.main.psu.PsuMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class PsoMain4Dir {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);

    /**
     * 主函数。
     *
     * @param args 第一个输入是配置文件所在的目录
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        File folder = new File(args[0]);
        File[] files = folder.listFiles();
        List<String> allFileName = new LinkedList<>(), failFileName = new LinkedList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    LOGGER.info(file.getName());
                    allFileName.add(file.getPath());
                }
            }
        }
        for(String fileName : allFileName){
            try{
                // read config
                LOGGER.info("read PTO config:{}", fileName);
                Properties properties = PropertiesUtils.loadProperties(fileName);
                // 读取协议类型
                String ptoType = PropertiesUtils.readString(properties, "pto_type");
                LOGGER.info("pto_type = " + ptoType);
                switch (ptoType) {
                    case PsuBlackIpMain.PTO_TYPE_NAME:
                        PsuBlackIpMain psuBlackIpMain = new PsuBlackIpMain(properties);
                        psuBlackIpMain.run();
                        break;
                    case PsuMain.PTO_TYPE_NAME:
                        PsuMain psuMain = new PsuMain(properties);
                        psuMain.runNetty();
                        break;
                    case PsiMain.PTO_TYPE_NAME:
                        PsiMain psiMain = new PsiMain(properties);
                        psiMain.runNetty();
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
                }
            }catch(Exception e){
                e.printStackTrace();
                failFileName.add(fileName);
            }
        }
        if(!failFileName.isEmpty()){
            LOGGER.error("those files fail:");
            for(String fileName : failFileName){
                LOGGER.error(fileName);
            }
        }
        System.exit(0);
    }
}
