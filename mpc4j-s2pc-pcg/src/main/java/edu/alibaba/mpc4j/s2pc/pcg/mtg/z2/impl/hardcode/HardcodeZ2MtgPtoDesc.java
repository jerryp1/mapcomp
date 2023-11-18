package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

import java.io.Serializable;

/**
 * cache Z2 multiplication triple generator protocol description.
 *
 * @author Li Peng
 * @date 2023/11/11
 */
class HardcodeZ2MtgPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4709553547410861136L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "HARDCODE_Z2_MTG";
    /**
     * singleton mode
     */
    private static final HardcodeZ2MtgPtoDesc INSTANCE = new HardcodeZ2MtgPtoDesc();

    /**
     * private constructor
     */
    private HardcodeZ2MtgPtoDesc() {
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
