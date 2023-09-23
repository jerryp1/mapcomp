package edu.alibaba.mpc4j.s2pc.pir.cppir.index.xospam;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * eXtreme One-time SPAM (XOSPAM) client-specific preprocessing PIR protocol description.
 *
 * @author Weiran Liu
 * @date 2023/9/23
 */
class XospamSingleIndexCpPirDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5853550184888102254L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LLP23_XO_SPAM";

    /**
     * the protocol step
     */
    enum PtoStep {
        /**
         * server sends the row stream database request
         */
        SERVER_SEND_ROW_STREAM_DATABASE_REQUEST,
        /**
         * client sends the row stream database response
         */
        CLIENT_SEND_MED_STREAM_DATABASE_RESPONSE,
        /**
         * server sends the column stream database request
         */
        SERVER_SEND_COLUMN_STREAM_DATABASE_REQUEST,
        /**
         * client sends the column stream database response
         */
        CLIENT_SEND_FINAL_STREAM_DATABASE_RESPONSE,
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
    private static final XospamSingleIndexCpPirDesc INSTANCE = new XospamSingleIndexCpPirDesc();

    /**
     * private constructor.
     */
    private XospamSingleIndexCpPirDesc() {
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
