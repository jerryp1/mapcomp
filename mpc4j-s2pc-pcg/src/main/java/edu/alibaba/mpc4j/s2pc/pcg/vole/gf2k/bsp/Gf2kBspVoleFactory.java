package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp;

import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * Batch single-point GF2K VOLE factory.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public class Gf2kBspVoleFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private Gf2kBspVoleFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Gf2kBspVoleType {
        /**
         * WYKW21 (semi-honest)
         */
        WYKW21_SEMI_HONEST,
        /**
         * WYKW21 (malicious)
         */
        WYKW21_MALICIOUS,
    }

    /**
     * Gets the pre-computed num.
     *
     * @param config config.
     * @param batch  batch num.
     * @param num    num.
     * @return pre-computed num.
     */
    public static int getPrecomputeNum(Gf2kBspVoleConfig config, int batch, int num) {
        assert num > 0 && batch > 0;
        Gf2kBspVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return batch;
            case WYKW21_MALICIOUS:
                return batch + 1;
            default:
                throw new IllegalArgumentException("Invalid " + Gf2kBspVoleType.class.getSimpleName() + ": " + type.name());
        }
    }
}
