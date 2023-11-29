package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.amos22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;


public class Amos22PrefixMaxPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6974961989782775600L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "AMOS23_PREFIX_MAX";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Amos22PrefixMaxPtoDesc INSTANCE = new Amos22PrefixMaxPtoDesc();

    /**
     * private constructor.
     */
    private Amos22PrefixMaxPtoDesc() {
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
