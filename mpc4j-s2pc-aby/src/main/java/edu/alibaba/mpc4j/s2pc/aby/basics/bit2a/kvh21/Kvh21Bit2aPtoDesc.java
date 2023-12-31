package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.kvh21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KVH+21 Bit2a protocol description. The protocol comes from the following paper:
 * <p>
 *
 * </p>
 *
 * @author Li Peng
 * @date 2023/10/12
 */
public class Kvh21Bit2aPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -583684506541113298L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "KVH+21_BIT2A";

    /**
     * protocol step
     */
    enum PtoStep {
        // obtain ote keys
        OBTAIN_OTE_KEYS,
        // sender send payloads
        SENDER_SEND_PAYLOADS,
    }

    /**
     * singleton mode
     */
    private static final Kvh21Bit2aPtoDesc INSTANCE =
        new Kvh21Bit2aPtoDesc();

    /**
     * private constructor.
     */
    private Kvh21Bit2aPtoDesc() {
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
