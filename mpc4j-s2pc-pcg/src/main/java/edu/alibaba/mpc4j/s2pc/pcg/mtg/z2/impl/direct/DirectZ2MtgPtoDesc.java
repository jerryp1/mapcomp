package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.direct;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * direct Boolean triple generation protocol description.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
class DirectZ2MtgPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1647712541830917487L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DIRECT_Z2_MTG";
    /**
     * singleton mode
     */
    private static final DirectZ2MtgPtoDesc INSTANCE = new DirectZ2MtgPtoDesc();

    /**
     * private constructor
     */
    private DirectZ2MtgPtoDesc() {
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
