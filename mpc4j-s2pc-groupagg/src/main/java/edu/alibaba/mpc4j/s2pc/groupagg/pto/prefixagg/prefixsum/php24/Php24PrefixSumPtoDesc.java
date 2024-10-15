package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixsum.php24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Php+24 prefix sum protocol description.
 *
 */
public class Php24PrefixSumPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4981462137146781151L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP24_PREFIX_SUM";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Php24PrefixSumPtoDesc INSTANCE = new Php24PrefixSumPtoDesc();

    /**
     * private constructor.
     */
    private Php24PrefixSumPtoDesc() {
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
