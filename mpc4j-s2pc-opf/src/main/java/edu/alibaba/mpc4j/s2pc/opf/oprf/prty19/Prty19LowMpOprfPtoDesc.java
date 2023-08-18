package edu.alibaba.mpc4j.s2pc.opf.oprf.prty19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PRTY19_LOW_OPRF的协议信息，通信量低的版本。论文来源：
 * <p>
 * Benny Pinkas, Mike Rosulek, et al. SpOT-Light- Lightweight Private Set Intersection from Sparse OT Extension.
 * CRYPTO 2019, pp. 401–431.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/17
 */
public class Prty19LowMpOprfPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)2763389634241267587L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "PRTY19_LOW_MPOPRF";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 接收方发送伪随机编码密钥
         */
        RECEIVER_SEND_OKVS_KEY,
        /**
         * 接收方发送OKVS Encoding
         */
        RECEIVER_SEND_STORAGE,
    }

    /**
     * 单例模式
     */
    private static final Prty19LowMpOprfPtoDesc INSTANCE = new Prty19LowMpOprfPtoDesc();

    /**
     * 私有构造函数
     */
    private Prty19LowMpOprfPtoDesc() {
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
