package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * XPIR protocol description. The protocol comes from the following paper:
 * <p>
 * Carlos Aguilar Melchor, Joris Barrier, Laurent Fousse, and Marc-Olivier Killijian. XPIR : Private Information
 * Retrieval for Everyone. Proc. Priv. Enhancing Technol. 2016, 2 (2016), 155–174
 * </p>
 * The original scheme was implemented using libNTL, here the SEAL implementation is applied.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class Mbfk16IndexPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5618466453562454763L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "XPIR";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * client send query
         */
        CLIENT_SEND_QUERY,
        /**
         * server send response
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * the singleton mode
     */
    private static final Mbfk16IndexPirPtoDesc INSTANCE = new Mbfk16IndexPirPtoDesc();

    /**
     * private constructor.
     */
    private Mbfk16IndexPirPtoDesc() {
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
