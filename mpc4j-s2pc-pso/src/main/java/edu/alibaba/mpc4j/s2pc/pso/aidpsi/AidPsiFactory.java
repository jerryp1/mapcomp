package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * aid PSI factory.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public class AidPsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private AidPsiFactory() {
        // empty
    }

    /**
     * aid PSI type.
     */
    public enum AidPsiType {
        /**
         * KMRS14 (semi-honest aider)
         */
        KMRS14_SH_AIDER,
        /**
         * KMRS14 (malicious aider)
         */
        KMRS14_MA_AIDER,
        /**
         * KMRS14 (intersection-size hiding malicious aider)
         */
        KMRS14_SIZE_HIDING_MA_AIDER,
    }
}
