package edu.alibaba.mpc4j.s2pc.opf.spermutation.xxx23b;

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
public class Xxx23bSharedPermutationPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4644370308367920585L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "XXX23B_SHARED_PERMUTATION";

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
    private static final Xxx23bSharedPermutationPtoDesc INSTANCE = new Xxx23bSharedPermutationPtoDesc();

    /**
     * private constructor.
     */
    private Xxx23bSharedPermutationPtoDesc() {
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
