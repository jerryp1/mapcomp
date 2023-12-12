package edu.alibaba.mpc4j.s2pc.opf.spermutation.php24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Php+24 shared permutation protocol description.
 *
 * @author Li Peng
 * @date 2023/5/25
 */
public class Php24SharedPermutationPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7002270011863181869L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP24_SHARED_PERMUTATION";

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
    private static final Php24SharedPermutationPtoDesc INSTANCE = new Php24SharedPermutationPtoDesc();

    /**
     * private constructor.
     */
    private Php24SharedPermutationPtoDesc() {
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
