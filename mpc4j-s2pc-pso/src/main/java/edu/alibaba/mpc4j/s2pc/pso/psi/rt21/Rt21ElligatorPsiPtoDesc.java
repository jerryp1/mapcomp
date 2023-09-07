package edu.alibaba.mpc4j.s2pc.pso.psi.rt21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RT21-PSI协议信息。论文来源：
 * <p>
 * Mike Rosulek and Ni Trieu. 2021.
 * Compact and Malicious Private Set Intersection for Small Sets.
 * In Proceedings of the 2021 ACM SIGSAC Conference on Computer and Communications Security (CCS '21).
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Rt21ElligatorPsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) -600495959111237630L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "RT21_PSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送msg(a)
         */
        SERVER_SEND_INIT,
        /**
         * 客户端发送P
         */
        CLIENT_SEND_POLY,
        /**
         * 服务端发送K
         */
        SERVER_SEND_KEYS,
    }

    /**
     * 单例模式
     */
    private static final Rt21ElligatorPsiPtoDesc INSTANCE = new Rt21ElligatorPsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Rt21ElligatorPsiPtoDesc() {
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
