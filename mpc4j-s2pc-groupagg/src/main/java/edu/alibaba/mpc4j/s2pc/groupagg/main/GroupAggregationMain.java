package edu.alibaba.mpc4j.s2pc.groupagg.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Group aggregation main class.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public class GroupAggregationMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupAggregationMain.class);

    private static final String COMMON_PARAMS = "common_params.txt";

    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // read configuration.
        LOGGER.info("read config file");

        String opt = args[0];
        switch (opt) {
            case "-s":
                processSingle(args);
                break;
            case "-m":
                processMul(args);
                break;
            default:
                throw new IllegalArgumentException("Invalid args:" + opt);
        }
    }

    /**
     * Handle single configuration.
     *
     * @param args arguments.
     */
    private static void processSingle(String[] args) throws IOException, URISyntaxException, MpcAbortException {
        Properties properties = PropertiesUtils.loadProperties(args[1]);
        Properties commonProperties = PropertiesUtils.loadProperties(args[1]);
        GroupAggregationStarter ptoStarter = new GroupAggregationStarter();
        ptoStarter.setProperties(properties);
        ptoStarter.setCommonProperties(commonProperties);
        ptoStarter.initRpc();
        ptoStarter.init();
        ptoStarter.start();
        ptoStarter.stopRpc();
        System.exit(0);
    }

    /**
     * Handle multiple configurations.
     *
     * @param args arguments.
     */
    private static void processMul(String[] args) throws IOException, URISyntaxException, MpcAbortException {
        List<String> files = listFilesForFolder(new File(args[1]));

        // read common params
        Properties commonProperties = null;
        for (String file : files) {
            if (file.contains(COMMON_PARAMS)) {
                commonProperties = PropertiesUtils.loadProperties(file);
                files.remove(file);
                break;
            }
        }
        if (commonProperties == null) {
            throw new IOException("common_params.txt is not found.");
        }
        // read experiment params
        files = files.stream().sorted().collect(Collectors.toList());
        List<Properties> ps = files.stream().map(PropertiesUtils::loadProperties).collect(Collectors.toList());
        GroupAggregationStarter ptoStarter = new GroupAggregationStarter();
        ptoStarter.setProperties(ps.get(0));
        ptoStarter.setCommonProperties(commonProperties);
        ptoStarter.initRpc();
        for (Properties p : ps) {
            ptoStarter.setProperties(p);
            ptoStarter.init();
            ptoStarter.start();
        }
        ptoStarter.stopRpc();
        System.exit(0);
    }

    /**
     * List files for specified folder.
     *
     * @param folder folder.
     * @return files.
     */
    public static List<String> listFilesForFolder(final File folder) {
        List<String> fileNames = new ArrayList<>();
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                if (fileEntry.getName().contains("txt")) {
                    fileNames.add(folder.getAbsolutePath() + "/" + fileEntry.getName());
                }
            }
        }
        return fileNames;
    }
}
