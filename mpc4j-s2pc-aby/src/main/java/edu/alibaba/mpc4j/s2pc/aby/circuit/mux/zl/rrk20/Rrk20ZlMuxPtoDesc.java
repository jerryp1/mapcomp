package edu.alibaba.mpc4j.s2pc.aby.circuit.mux.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RRK+20 Zl mux protocol description. The protocol comes from Appendix A.3 of the following paper:
 * <p>
 * Rathee, Deevashwer, Mayank Rathee, Nishant Kumar, Nishanth Chandran, Divya Gupta, Aseem Rastogi, and Rahul Sharma.
 * CrypTFlow2: Practical 2-party secure inference. CCS 2020, pp. 325-342. 2020.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
class Rrk20ZlMuxPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2557871605895618929L);
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
    private static final Rrk20ZlMuxPtoDesc INSTANCE = new Rrk20ZlMuxPtoDesc();

    /**
     * 私有构造函数
     */
    private Rrk20ZlMuxPtoDesc() {
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
