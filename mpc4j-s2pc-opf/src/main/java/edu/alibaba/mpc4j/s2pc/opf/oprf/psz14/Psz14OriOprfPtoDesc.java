package edu.alibaba.mpc4j.s2pc.opf.oprf.psz14;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PSZ14 origin方案的OPRF协议描述信息，论文为：
 * <p>
 * Benny Pinkas and Thomas Schneider and Michael Zohner Faster Private Set Intersection Based on OT Extension.
 * USENIX Security 2014, pp. 797--812.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/16
 */
public class Psz14OriOprfPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)-2789554629844311867L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "PSZ14_ORI_OPRF";

    /**
     * 协议步骤
     */
    enum PtoStep {

    }
    /**
     * 单例模式
     */
    private static final Psz14OriOprfPtoDesc INSTANCE = new Psz14OriOprfPtoDesc();

    /**
     * 私有构造函数
     */
    private Psz14OriOprfPtoDesc() {
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
