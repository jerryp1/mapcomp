package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * Single single-point GF2K VOLE factory.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class SspGf2kVoleFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SspGf2kVoleFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum SspGf2kVoleType {
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
     * @param config the config.
     * @param num    num.
     * @return the pre-computed num.
     */
    public static int getPrecomputeNum(SspGf2kVoleConfig config, int num) {
        assert num > 0 : "num must be greater than 0: " + num;
        SspGf2kVoleType type = config.getPtoType();
        switch (type) {
            case WYKW21_SEMI_HONEST:
                return 1;
            case WYKW21_MALICIOUS:
                return 2;
            default:
                throw new IllegalArgumentException("Invalid " + SspGf2kVoleType.class.getSimpleName() + ": " + type.name());
        }
    }
}
