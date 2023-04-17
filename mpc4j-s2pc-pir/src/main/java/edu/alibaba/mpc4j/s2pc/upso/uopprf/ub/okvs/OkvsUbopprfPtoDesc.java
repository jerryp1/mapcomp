package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * OKVS unbalanced batched OPRRF protocol description.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
class OkvsUbopprfPtoDesc implements PtoDesc {
    /**
     * the protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2740061226839221782L);
    /**
     * the protocol name
     */
    private static final String PTO_NAME = "OKVS_UBOPPRF";
    /**
     * the singleton mode
     */
    private static final OkvsUbopprfPtoDesc INSTANCE = new OkvsUbopprfPtoDesc();

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * the sender sends OKVS keys
         */
        SENDER_SEND_OKVS_KEYS,
        /**
         * the sender sends OKVS
         */
        SENDER_SEND_OKVS,
    }

    /**
     * private constructor.
     */
    private OkvsUbopprfPtoDesc() {
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
