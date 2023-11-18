package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.xxx23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Plain and protocol description. The protocol comes from Appendix A of the following paper:
 * <p>
 * </p>
 *
 * @author Li Peng
 * @date 2023/11/8
 */
class Xxx23PlainAndPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -2810215118024216597L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "XXX23_PLAIN_AND";

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
    private static final Xxx23PlainAndPtoDesc INSTANCE = new Xxx23PlainAndPtoDesc();

    /**
     * private constructor.
     */
    private Xxx23PlainAndPtoDesc() {
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
