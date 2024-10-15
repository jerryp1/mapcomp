package edu.alibaba.mpc4j.s2pc.opf.permutation.php24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Php+24 permutation protocol description.
 *
 */
public class Php24PermutationPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8140199955410366544L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP+24_PERMUTATION";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Php24PermutationPtoDesc INSTANCE = new Php24PermutationPtoDesc();

    /**
     * private constructor.
     */
    private Php24PermutationPtoDesc() {
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
