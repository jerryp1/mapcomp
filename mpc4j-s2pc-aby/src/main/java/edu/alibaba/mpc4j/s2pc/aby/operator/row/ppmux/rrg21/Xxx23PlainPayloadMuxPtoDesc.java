package edu.alibaba.mpc4j.s2pc.aby.operator.row.ppmux.rrg21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Plain mux protocol description. The protocol comes from Appendix A of the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/11/5
 */
class Xxx23PlainPayloadMuxPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -1299787225884780879L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "XXX23_PLAIN_PAYLOAD_MUX";

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
    private static final Xxx23PlainPayloadMuxPtoDesc INSTANCE = new Xxx23PlainPayloadMuxPtoDesc();

    /**
     * private constructor.
     */
    private Xxx23PlainPayloadMuxPtoDesc() {
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
