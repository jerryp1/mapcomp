package edu.alibaba.mpc4j.s2pc.opf.spermutation.php24b;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Php+24 shared permutation protocol description.
 *
 * @author Li Peng
 * @date 2023/5/25
 */
public class Php24bSharedPermutationPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4644370308367920585L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP24B_SHARED_PERMUTATION";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * Reveal data step1
         */
        REVEAL1,
        /**
         * Reveal data step2
         */
        REVEAL2,
    }

    /**
     * singleton mode
     */
    private static final Php24bSharedPermutationPtoDesc INSTANCE = new Php24bSharedPermutationPtoDesc();

    /**
     * private constructor.
     */
    private Php24bSharedPermutationPtoDesc() {
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
