package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg.mix;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Mix group aggregation protocol description. The protocol comes from the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public class MixGroupAggPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -985239413597525368L);
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
    private static final MixGroupAggPtoDesc INSTANCE = new MixGroupAggPtoDesc();

    /**
     * private constructor.
     */
    private MixGroupAggPtoDesc() {
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
