package edu.alibaba.mpc4j.s2pc.aby.basics.bit2a.tuple;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

import java.io.Serializable;

/**
 * Tuple Bit2a protocol description. The protocol comes from the following paper:
 * <p>
 *
 * </p>
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public class TupleBit2aPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 1683890901205554855L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "TUPLE_BIT2A";

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
    private static final TupleBit2aPtoDesc INSTANCE =
        new TupleBit2aPtoDesc();

    /**
     * private constructor.
     */
    private TupleBit2aPtoDesc() {
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
