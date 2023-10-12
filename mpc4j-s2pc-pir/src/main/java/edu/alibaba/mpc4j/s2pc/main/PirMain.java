package edu.alibaba.mpc4j.s2pc.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.main.batchindex.BatchIndexPirMain;
import edu.alibaba.mpc4j.s2pc.main.ccpsi.CcpsiMain;
import edu.alibaba.mpc4j.s2pc.main.payablepir.PayablePirMain;
import edu.alibaba.mpc4j.s2pc.main.scpsi.ScpsiMain;
import edu.alibaba.mpc4j.s2pc.main.ucpsi.UcpsiMain;
import edu.alibaba.mpc4j.s2pc.main.cppir.index.IndexCppirMain;
import edu.alibaba.mpc4j.s2pc.main.cppir.keyword.KeywordCppirMain;
import edu.alibaba.mpc4j.s2pc.main.kwpir.KwPirMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * PIR main.
 *
 * @author Liqiang Peng
 * @date 2022/04/23
 */
public class PirMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PirMain.class);

    /**
     * main.
     *
     * @param args one input: config file name.
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // read config file
        LOGGER.info("read PTO config");
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        // read task type
        String taskType = PropertiesUtils.readString(properties, "task_type");
        LOGGER.info("task_type = " + taskType);
        switch (taskType) {
            case UcpsiMain.TASK_NAME:
                UcpsiMain ucpsiMain = new UcpsiMain(properties);
                ucpsiMain.run();
                break;
            case ScpsiMain.TASK_NAME:
                ScpsiMain scpsiMain = new ScpsiMain(properties);
                scpsiMain.run();
                break;
            case CcpsiMain.TASK_NAME:
                CcpsiMain ccpsiMain = new CcpsiMain(properties);
                ccpsiMain.run();
                break;
            case BatchIndexPirMain.TASK_NAME:
                BatchIndexPirMain batchIndexPirMain = new BatchIndexPirMain(properties);
                batchIndexPirMain.run();
                break;
            case IndexCppirMain.TASK_NAME:
                IndexCppirMain indexCppirMain = new IndexCppirMain(properties);
                indexCppirMain.run();
                break;
            case KeywordCppirMain.TASK_NAME:
                KeywordCppirMain keywordCppirMain = new KeywordCppirMain(properties);
                keywordCppirMain.run();
                break;
            case KwPirMain.TASK_NAME:
                KwPirMain kwPirMain = new KwPirMain(properties);
                kwPirMain.run();
                break;
            case PayablePirMain.TASK_NAME:
                PayablePirMain payablePirMain = new PayablePirMain(properties);
                payablePirMain.run();
                break;
            default:
                throw new IllegalArgumentException("Invalid task_type: " + taskType);
        }
        System.exit(0);
    }
}
