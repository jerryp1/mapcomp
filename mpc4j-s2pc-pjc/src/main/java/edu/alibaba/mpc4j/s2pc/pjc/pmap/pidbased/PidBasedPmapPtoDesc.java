package edu.alibaba.mpc4j.s2pc.pjc.pmap.pidbased;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PID-based map protocol description, the protocol comes from "MapComp"
 *
 * @author Feng Han
 * @date 2023/11/20
 */
public class PidBasedPmapPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -2607344576398744169L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PID_PMAP";

    /**
     * singleton mode
     */
    private static final PidBasedPmapPtoDesc INSTANCE = new PidBasedPmapPtoDesc();

    public enum PtoStep {
        /**
         * the client sends hash keys
         */
        SERVER_SEND_HASH_KEYS,
    }

    /**
     * private constructor
     */
    private PidBasedPmapPtoDesc() {
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
