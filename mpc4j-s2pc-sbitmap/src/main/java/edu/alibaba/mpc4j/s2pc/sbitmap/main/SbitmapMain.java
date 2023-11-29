package edu.alibaba.mpc4j.s2pc.sbitmap.main;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Sbitmap main class.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
public class SbitmapMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(SbitmapMain.class);

    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        // read configuration.
        LOGGER.info("read config file");

        List<String> files = listFilesForFolder(new File(args[0]));
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

    public static List<String> listFilesForFolder(final File folder) {
        List<String> fileNames = new ArrayList<>();
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                fileNames.add(folder.getAbsolutePath() + "/" + fileEntry.getName());
            }
        }
        return fileNames;
    }
}
