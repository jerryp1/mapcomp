package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.php24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Plain mux protocol description.
 *
 * @author Li Peng
 * @date 2023/11/5
 */
class Php24PlainPayloadMuxPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -1299787225884780879L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP+24_PLAIN_PAYLOAD_MUX";

    /**
     * protocol step
     */
    enum PtoStep {
        // sender send payloads
        SENDER_SEND_PAYLOADS,
    }

    /**
     * singleton mode
     */
    private static final Php24PlainPayloadMuxPtoDesc INSTANCE = new Php24PlainPayloadMuxPtoDesc();

    /**
     * private constructor.
     */
    private Php24PlainPayloadMuxPtoDesc() {
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
