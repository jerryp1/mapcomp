package edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.z2.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class Rrk20Z2MuxPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8108739059852538512L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RRK+20_ZL_MUX";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * the sender sends s0 and s1
         */
        SENDER_SEND_S0_S1,
        /**
         * the receiver sends t0 and t1
         */
        RECEIVER_SEND_T0_T1,
    }

    /**
     * singleton mode
     */
    private static final Rrk20Z2MuxPtoDesc INSTANCE = new Rrk20Z2MuxPtoDesc();

    /**
     * private constructor.
     */
    private Rrk20Z2MuxPtoDesc() {
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
