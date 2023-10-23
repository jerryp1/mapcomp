package edu.alibaba.mpc4j.s2pc.opf.permutation.xxx23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Xxx23 permutation protocol description. The protocol comes from the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class Xxx23PermutationPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8140199955410366544L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "XXX23_PERMUTATION";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Xxx23PermutationPtoDesc INSTANCE = new Xxx23PermutationPtoDesc();

    /**
     * private constructor.
     */
    private Xxx23PermutationPtoDesc() {
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
