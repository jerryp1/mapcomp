package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.kk13;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;


/**
 * KK13-NC-BitOT协议描述。
 * 将N选1-OT转换为log N个2选1-BitOT。论文KK13采用该方法但未详细给出实现细节。论文信息：
 *  <p>
 *  Kolesnikov V, Kumaresan R. Improved OT Extension for Transferring Short Secrets. CRYPTO 2013, Springer, Berlin,
 *  Heidelberg, 2013, pp. 54-70.
 *  </p>
 *
 *
 * @author Hanwen Feng
 * @date 2022/08/18
 */
public class Kk13NcBitOtPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 4537625957898269445L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "KK13-NC-BitOT";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送密文
         */
        SENDER_SEND_CIPHER,
    }

    /**
     * 单例模式
     */
    private static final Kk13NcBitOtPtoDesc INSTANCE = new Kk13NcBitOtPtoDesc();

    /**
     * 私有构造函数
     */
    private Kk13NcBitOtPtoDesc() {
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
