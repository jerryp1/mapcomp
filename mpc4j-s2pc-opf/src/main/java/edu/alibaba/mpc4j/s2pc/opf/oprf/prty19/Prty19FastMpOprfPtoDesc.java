package edu.alibaba.mpc4j.s2pc.opf.oprf.prty19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class Prty19FastMpOprfPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 9103839597754023837L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "PRTY19FAST_MPOPRF";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送伪随机编码密钥
         */
        RECEIVER_SEND_KEY,
        /**
         * 接收方发送OKVS Encoding
         */
        RECEIVER_SEND_STORAGE,
    }

    /**
     * 单例模式
     */
    private static final Prty19FastMpOprfPtoDesc INSTANCE = new Prty19FastMpOprfPtoDesc();

    /**
     * 私有构造函数
     */
    private Prty19FastMpOprfPtoDesc() {
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
