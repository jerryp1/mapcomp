package edu.alibaba.mpc4j.s2pc.aby.basics.b2a.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * DSZ15 B2a protocol description. The protocol comes from the following paper:
 * <p>
 * Demmler, Daniel, Thomas Schneider, and Michael Zohner. "ABY-A framework for efficient mixed-protocol
 * secure two-party computation." NDSS. 2015.
 * </p>
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public class Dsz15B2aPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -3704115308334790390L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DSZ15_B2A";

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
    private static final Dsz15B2aPtoDesc INSTANCE =
        new Dsz15B2aPtoDesc();

    /**
     * private constructor.
     */
    private Dsz15B2aPtoDesc() {
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
