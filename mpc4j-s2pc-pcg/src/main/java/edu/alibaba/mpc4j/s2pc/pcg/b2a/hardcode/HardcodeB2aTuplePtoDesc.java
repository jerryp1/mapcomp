package edu.alibaba.mpc4j.s2pc.pcg.b2a.hardcode;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

import java.io.Serializable;

/**
 * cache Z2 multiplication triple generator protocol description.
 *
 * @author Li Peng
 * @date 2023/11/21
 */
class HardcodeB2aTuplePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -6100152853028069021L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "HARDCODE_B2A_TUPLE";
    /**
     * singleton mode
     */
    private static final HardcodeB2aTuplePtoDesc INSTANCE = new HardcodeB2aTuplePtoDesc();

    /**
     * private constructor
     */
    private HardcodeB2aTuplePtoDesc() {
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
