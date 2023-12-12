package edu.alibaba.mpc4j.s2pc.sbitmap.main;

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
        SbitmapStarter fullSecureProtocol = new SbitmapStarter();
        fullSecureProtocol.setProperties(properties);
        fullSecureProtocol.initRpc();
        fullSecureProtocol.init();
        fullSecureProtocol.start();
        fullSecureProtocol.stopRpc();
        System.exit(0);
    }

    /**
     * Handle multiple configurations.
     *
     * @param args arguments.
     */
    private static void processMul(String[] args) throws IOException, URISyntaxException, MpcAbortException {
        List<String> files = listFilesForFolder(new File(args[1]));
        files = files.stream().sorted().collect(Collectors.toList());
        List<Properties> ps = files.stream().map(PropertiesUtils::loadProperties).collect(Collectors.toList());
        SbitmapStarter fullSecureProtocol = new SbitmapStarter();
        fullSecureProtocol.setProperties(ps.get(0));
        fullSecureProtocol.initRpc();
        for (Properties p : ps) {
            fullSecureProtocol.setProperties(p);
            fullSecureProtocol.init();
            fullSecureProtocol.start();
        }
        fullSecureProtocol.stopRpc();
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
