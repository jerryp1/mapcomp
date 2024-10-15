package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.omix;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Optimized mix group aggregation protocol description. The protocol comes from the following paper:
 * <p>
 * </p>
 */
public class OptimizedMixGroupAggPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -1323578036618123497L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "O_MIX_GROUP_AGG";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
        REVEAL_OUTPUT
    }

    /**
     * singleton mode
     */
    private static final OptimizedMixGroupAggPtoDesc INSTANCE = new OptimizedMixGroupAggPtoDesc();

    /**
     * private constructor.
     */
    private OptimizedMixGroupAggPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
