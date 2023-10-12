package edu.alibaba.mpc4j.s2pc.main.cppir.index;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.shuffle.ShuffleSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleIndexCpPirConfig;

import java.util.Properties;

import static edu.alibaba.mpc4j.s2pc.pir.cppir.index.SingleIndexCpPirFactory.*;

/**
 * Index CPPIR config utils.
 *
 * @author Liqiang Peng
 * @date 2023/9/26
 */
public class IndexCppirConfigUtils {

    private IndexCppirConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static SingleIndexCpPirConfig createIndexCpPirConfig(Properties properties) {
        // read protocol type
        String indexCppirTypeString = PropertiesUtils.readString(properties, "pto_name");
        SingleIndexCpPirType indexCpPirType = SingleIndexCpPirType.valueOf(indexCppirTypeString);
        switch (indexCpPirType) {
            case ZPSZ23_PIANO:
                return new PianoSingleIndexCpPirConfig.Builder().build();
            case MIR23_SPAM:
                return new SpamSingleIndexCpPirConfig.Builder().build();
            case HHCM23_SIMPLE:
                return new SimpleSingleIndexCpPirConfig.Builder().build();
            case LLP23_SHUFFLE:
                return new ShuffleSingleIndexCpPirConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleIndexCpPirType.class.getSimpleName() + ": " + indexCpPirType.name()
                );
        }
    }
}