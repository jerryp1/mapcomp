package edu.alibaba.mpc4j.s2pc.opf.permutation.php24b;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Php+24 permutation protocol description.
 */
public class Php24bPermutationPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -2873563970626009534L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP24B_PERMUTATION";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Php24bPermutationPtoDesc INSTANCE = new Php24bPermutationPtoDesc();

    /**
     * private constructor.
     */
    private Php24bPermutationPtoDesc() {
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
