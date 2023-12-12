package edu.alibaba.mpc4j.s2pc.pjc.pmap.hpl24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * level 2 secure 的private map协议信息。方案来自于MapComp
 *
 * @author Feng Han
 * @date 2023/10/24
 */
public class Hpl24PmapPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) -3452608700023484963L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "HPL24_PMAP";

    /**
     * 单例模式
     */
    private static final Hpl24PmapPtoDesc INSTANCE = new Hpl24PmapPtoDesc();

    /**
     * 私有构造函数
     */
    private Hpl24PmapPtoDesc() {
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
