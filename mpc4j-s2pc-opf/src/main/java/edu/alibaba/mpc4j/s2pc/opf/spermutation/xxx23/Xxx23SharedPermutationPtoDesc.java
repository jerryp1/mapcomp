package edu.alibaba.mpc4j.s2pc.opf.spermutation.xxx23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Xxx+23 shared permutation protocol description. The protocol comes from the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/5/25
 */
public class Xxx23SharedPermutationPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7002270011863181869L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "XXX23_SHARED_PERMUTATION";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
        REVEAL1,
        REVEAL2,
    }

    /**
     * singleton mode
     */
    private static final Xxx23SharedPermutationPtoDesc INSTANCE = new Xxx23SharedPermutationPtoDesc();

    /**
     * private constructor.
     */
    private Xxx23SharedPermutationPtoDesc() {
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
