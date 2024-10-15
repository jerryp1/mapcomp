package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.prefixxor.php24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Php+24 prefix xor protocol description.
 *
 */
public class Php24PrefixXorPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4981362137146781151L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP24_PREFIX_XOR";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Php24PrefixXorPtoDesc INSTANCE = new Php24PrefixXorPtoDesc();

    /**
     * private constructor.
     */
    private Php24PrefixXorPtoDesc() {
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
