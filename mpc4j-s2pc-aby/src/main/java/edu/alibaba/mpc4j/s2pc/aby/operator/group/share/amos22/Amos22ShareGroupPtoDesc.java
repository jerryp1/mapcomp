package edu.alibaba.mpc4j.s2pc.aby.operator.group.share.amos22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class Amos22ShareGroupPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -4722715040547974777L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "AMOS22_SHARE";

    /**
     * singleton mode
     */
    private static final Amos22ShareGroupPtoDesc INSTANCE =
        new Amos22ShareGroupPtoDesc();

    /**
     * private constructor.
     */
    private Amos22ShareGroupPtoDesc() {
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
