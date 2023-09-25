package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.shuffle;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Shuffle client-specific preprocessing PIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2023/9/20
 */
public class ShuffleSingleKeywordCpPirDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6665219932164077804L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LLP23_SHUFFLE";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server send blind
         */
        SERVER_SEND_BLIND,
        /**
         * client send blind prf
         */
        CLIENT_SEND_BLIND_PRF,
        /**
         * client send query
         */
        CLIENT_SEND_QUERY,
        /**
         * server send item response
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * the singleton mode
     */
    private static final ShuffleSingleKeywordCpPirDesc INSTANCE = new ShuffleSingleKeywordCpPirDesc();

    /**
     * private constructor.
     */
    private ShuffleSingleKeywordCpPirDesc() {
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
