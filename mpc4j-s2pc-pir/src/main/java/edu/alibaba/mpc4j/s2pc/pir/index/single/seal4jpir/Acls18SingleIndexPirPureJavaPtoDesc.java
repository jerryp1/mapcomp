package edu.alibaba.mpc4j.s2pc.pir.index.single.seal4jpir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * @author Qixian Zhou
 * @date 2023/10/13
 */
public class Acls18SingleIndexPirPureJavaPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7757109062183018060L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SEAL4J_PIR";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * client sends public keys
         */
        CLIENT_SEND_PUBLIC_KEYS,
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
    private static final Acls18SingleIndexPirPureJavaPtoDesc INSTANCE = new Acls18SingleIndexPirPureJavaPtoDesc();

    /**
     * private constructor.
     */
    private Acls18SingleIndexPirPureJavaPtoDesc() {
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
