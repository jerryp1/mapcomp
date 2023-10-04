package edu.alibaba.mpc4j.s2pc.main.cppir.keyword;

import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleKeywordCpPirFactory.SingleKeywordCpPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleKeywordCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.shuffle.ShuffleSingleKeywordCpPirConfig;

import java.util.Properties;

/**
 * Keyword CPPIR config utils.
 *
 * @author Liqiang Peng
 * @date 2023/9/27
 */
public class KeywordCppirConfigUtils {

    private KeywordCppirConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */
    public static SingleKeywordCpPirConfig createKeywordCpPirConfig(Properties properties) {
        // read protocol type
        String keywordCppirTypeString = PropertiesUtils.readString(properties, "pto_name");
        KeywordCppirType keywordCppirType = KeywordCppirType.valueOf(keywordCppirTypeString);
        switch (keywordCppirType) {
            case ZPSZ23_PIANO:
                return new Alpr21SingleKeywordCpPirConfig.Builder()
                    .setSingleIndexCpPirConfig(new PianoSingleIndexCpPirConfig.Builder().build())
                    .build();
            case MIR23_SPAM:
                return new Alpr21SingleKeywordCpPirConfig.Builder()
                    .setSingleIndexCpPirConfig(new SpamSingleIndexCpPirConfig.Builder().build())
                    .build();
            case HHCM23_SIMPLE:
                return new Alpr21SingleKeywordCpPirConfig.Builder()
                    .setSingleIndexCpPirConfig(new SimpleSingleIndexCpPirConfig.Builder().build())
                    .build();
            case LLP23_SHUFFLE:
                return new ShuffleSingleKeywordCpPirConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleKeywordCpPirType.class.getSimpleName() + ": " + keywordCppirType.name()
                );
        }
    }
}