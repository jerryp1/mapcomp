package edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.Xxx23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

import java.io.Serializable;

/**
 * Plain bit mux protocol description. The protocol comes from Appendix A of the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/11/5
 */
class Xxx23PlainBitMuxPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4075752027088262232L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "XXX23_PLAIN_BIT_MUX";

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
    private static final Xxx23PlainBitMuxPtoDesc INSTANCE = new Xxx23PlainBitMuxPtoDesc();

    /**
     * private constructor.
     */
    private Xxx23PlainBitMuxPtoDesc() {
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
