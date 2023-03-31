package edu.alibaba.mpc4j.s2pc.pso.cpsi;

/**
 * Circuit PSI factory.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class CpsiFactory {
    /**
     * private constructor.
     */
    private CpsiFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum CpsiType {
        /**
         * PSTY19 circuit PSI
         */
        PSTY19,
        /**
         * RS21 circuit PSI
         */
        RS21,
        /**
         * CGS22 circuit PSI
         */
        CGS22,
    }
}
