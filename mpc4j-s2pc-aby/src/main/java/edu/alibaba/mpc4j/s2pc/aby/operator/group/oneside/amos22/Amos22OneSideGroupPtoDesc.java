package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class Amos22OneSideGroupPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5961670529002355579L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "AMOS22_ONE_SIDE";

    /**
     * singleton mode
     */
    private static final Amos22OneSideGroupPtoDesc INSTANCE =
        new Amos22OneSideGroupPtoDesc();

    /**
     * private constructor.
     */
    private Amos22OneSideGroupPtoDesc() {
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
