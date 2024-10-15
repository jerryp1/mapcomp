package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.oneside;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * One-side group aggregation protocol description, which is specially used for one-side TPC-H test.
 */
public class OnesideGroupAggPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -985239213597525368L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "MIX_GROUP_AGG";

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
    private static final OnesideGroupAggPtoDesc INSTANCE = new OnesideGroupAggPtoDesc();

    /**
     * private constructor.
     */
    private OnesideGroupAggPtoDesc() {
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
