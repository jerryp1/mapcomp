package edu.alibaba.mpc4j.s2pc.aby.operator.agg.prefixsum.xxx23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Xxx23 prefix sum protocol description. The protocol comes from Algorithm 2 of the following paper:
 * <p>
 *
 * </p>
 *
 * @author Li Peng
 * @date 2023/5/30
 */
public class Xxx23PrefixSumPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4981462137146781151L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "XXX23_PREFIX_SUM";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Xxx23PrefixSumPtoDesc INSTANCE = new Xxx23PrefixSumPtoDesc();

    /**
     * private constructor.
     */
    private Xxx23PrefixSumPtoDesc() {
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
