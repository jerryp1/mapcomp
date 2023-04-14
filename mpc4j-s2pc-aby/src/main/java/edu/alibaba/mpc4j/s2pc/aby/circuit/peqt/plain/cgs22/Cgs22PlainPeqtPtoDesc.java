package edu.alibaba.mpc4j.s2pc.aby.circuit.peqt.plain.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * CGS22 plain private equality test protocol description. The protocol is described in Fig. 6 of the following paper:
 * <p>
 * Chandran, Nishanth, Divya Gupta, and Akash Shah. Circuit-PSI With Linear Complexity via Relaxed Batch OPPRF.
 * PETS 2022, pp. 353-372.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
class Cgs22PlainPeqtPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7631840207052425416L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CGS22_PLAIN_PEQT";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * the sender sends equality payloads
         */
        SENDER_SEND_EVS,
    }

    /**
     * singleton mode
     */
    private static final Cgs22PlainPeqtPtoDesc INSTANCE = new Cgs22PlainPeqtPtoDesc();

    /**
     * private constructor.
     */
    private Cgs22PlainPeqtPtoDesc() {
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
