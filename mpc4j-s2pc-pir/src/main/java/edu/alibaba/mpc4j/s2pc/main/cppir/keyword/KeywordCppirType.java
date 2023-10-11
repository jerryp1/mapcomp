package edu.alibaba.mpc4j.s2pc.main.cppir.keyword;

/**
 * Keyword CPPIR type.
 *
 * @author Liqiang Peng
 * @date 2023/9/27
 */
public enum KeywordCppirType {
    /**
     * shuffle
     */
    LLP23_SHUFFLE,
    /**
     * ALPR21 + shuffle
     */
    LLP23_INDEX_SHUFFLE,
    /**
     * ALPR21 + PIANO
     */
    ZPSZ23_PIANO,
    /**
     * ALPR21 + SPAM
     */
    MIR23_SPAM,
    /**
     * ALPR21 + SIMPLE PIR
     */
    HHCM23_SIMPLE,
}
