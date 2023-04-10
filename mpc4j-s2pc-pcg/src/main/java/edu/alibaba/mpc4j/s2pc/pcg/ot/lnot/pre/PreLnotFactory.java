package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.pre;

import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * pre-compute 1-out-of-n (with n = 2^l) OT factory.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class PreLnotFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PreLnotFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum PreLnotType {
        /**
         * Bea95
         */
        Bea95,
    }
}
