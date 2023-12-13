package edu.alibaba.mpc4j.s2pc.opf.prefixagg.prefixmax.php24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Php+24 prefix max protocol description.
 *
 * @author Li Peng
 * @date 2023/11/1
 */
public class Php24PrefixMaxPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 853402398593947956L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP24_PREFIX_MAX";

    /**
     * protocol step
     */
    enum PtoStep {
        // empty
    }

    /**
     * singleton mode
     */
    private static final Php24PrefixMaxPtoDesc INSTANCE = new Php24PrefixMaxPtoDesc();

    /**
     * private constructor.
     */
    private Php24PrefixMaxPtoDesc() {
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
