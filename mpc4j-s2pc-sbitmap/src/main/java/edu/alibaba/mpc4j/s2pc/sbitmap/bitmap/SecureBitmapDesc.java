package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.core.kos16.Kos16ZpCoreVolePtoDesc;

import java.io.Serializable;

/**
 * @author Li Peng
 * @date 2023/8/17
 */
public class SecureBitmapDesc implements PtoDesc, Serializable {
    /**
     * 协议ID。
     */
    private static final int PTO_ID = Math.abs((int) 2047864806225283373L);
    /**
     * 协议名称。
     */
    private static final String PTO_NAME = "KOS16_ZP_CORE_VOLE";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 发送方发送矩阵
         */
        SENDER_SEND_KEYS,
    }

    /**
     * 单例模式。
     */
    private static final SecureBitmapDesc INSTANCE = new SecureBitmapDesc();

    /**
     * 私有构造函数。
     */
    private SecureBitmapDesc() {
        // empty
    }

    /**
     * 获取静态实例。
     */
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
