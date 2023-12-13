package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.php24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Plain and protocol description.
 *
 * @author Li Peng
 * @date 2023/11/8
 */
class Php24PlainAndPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -2810215118024216597L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PHP+24_PLAIN_AND";

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
    private static final Php24PlainAndPtoDesc INSTANCE = new Php24PlainAndPtoDesc();

    /**
     * private constructor.
     */
    private Php24PlainAndPtoDesc() {
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
