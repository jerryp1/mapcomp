package edu.alibaba.mpc4j.s2pc.opf.sqoprf.ecdh;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public class EcdhEccSqOprfPtoDesc implements PtoDesc{


    /**
     * 协议ID random get a  long value as PTO_ID
     */
    private static final int PTO_ID = Math.abs((int)7777433025490717147L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "ECDH_ECC_SQ_OPRF";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送盲化元素
         */
        RECEIVER_SEND_BLIND,
        /**
         * 发送方返回接收方盲化元素PRF
         */
        SENDER_SEND_BLIND_PRF,
    }

    /**
     * 单例模式
     */
    private static final EcdhEccSqOprfPtoDesc INSTANCE = new EcdhEccSqOprfPtoDesc();

    /**
     * 私有构造函数
     */
    private EcdhEccSqOprfPtoDesc() {
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
