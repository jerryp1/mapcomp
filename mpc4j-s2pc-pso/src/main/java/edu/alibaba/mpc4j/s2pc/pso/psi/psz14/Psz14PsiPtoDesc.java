package edu.alibaba.mpc4j.s2pc.pso.psi.psz14;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PSZ14优化版本的PSI协议描述信息，论文为：
 * <p>
 * Benny Pinkas and Thomas Schneider and Michael Zohner Faster Private Set Intersection Based on OT Extension.
 * USENIX Security 2014, pp. 797--812.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/16
 */
public class Psz14PsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 6557483477744526789L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "PSZ14";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 客户端发送布谷鸟哈希密钥
         */
        CLIENT_SEND_CUCKOO_HASH_KEYS,
        /**
         * 服务端发送哈希桶PRF
         */
        SERVER_SEND_BIN_PRFS,
        /**
         * 服务端发送贮存区PRF
         */
        SERVER_SEND_STASH_PRFS,
    }

    /**
     * 单例模式
     */
    private static final Psz14PsiPtoDesc INSTANCE = new Psz14PsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Psz14PsiPtoDesc() {
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