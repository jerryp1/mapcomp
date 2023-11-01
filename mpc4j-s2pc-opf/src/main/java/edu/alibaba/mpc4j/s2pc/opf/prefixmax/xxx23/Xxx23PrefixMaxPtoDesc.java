package edu.alibaba.mpc4j.s2pc.opf.prefixmax.xxx23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

import java.io.Serializable;

/**
 * Xxx23 prefix max protocol description. The protocol comes from the following paper:
 * <p>
 *
 * </p>
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public class Xxx23PrefixMaxPtoDesc implements PtoDesc, Serializable {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -8073511943334422743L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "XXX23_PREFIX_MAX";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Xxx23PrefixMaxPtoDesc INSTANCE = new Xxx23PrefixMaxPtoDesc();

    /**
     * private constructor.
     */
    private Xxx23PrefixMaxPtoDesc() {
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
