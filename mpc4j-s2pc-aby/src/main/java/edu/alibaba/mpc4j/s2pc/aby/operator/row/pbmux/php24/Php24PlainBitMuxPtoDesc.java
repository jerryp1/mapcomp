package edu.alibaba.mpc4j.s2pc.aby.operator.row.pbmux.php24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Plain bit mux protocol description. The protocol comes from Appendix A of the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/11/5
 */
class Php24PlainBitMuxPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 4075752027088262232L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP+24_PLAIN_BIT_MUX";

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
    private static final Php24PlainBitMuxPtoDesc INSTANCE = new Php24PlainBitMuxPtoDesc();

    /**
     * private constructor.
     */
    private Php24PlainBitMuxPtoDesc() {
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
