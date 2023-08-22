package edu.alibaba.mpc4j.s2pc.pso.mppsi;

import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

public class MppsiFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private MppsiFactory() {
        // empty
    }

    /**
     * MP PSI协议类型。
     */
    public enum MppsiType {
        /**
         * KMPRT17 augmented semi-honest方案
         */
        KMPRT17_AUGMENTED_SEMIHONEST,
        /**
         * KMPRT17 semi-honest方案
         */
        KMPRT17,
    }
}
