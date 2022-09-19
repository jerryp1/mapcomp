package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

/**
 * PSI协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class PsiFactory {
    /**
     * 私有构造函数
     */
    private PsiFactory() {
        // empty
    }

    /**
     * PSI协议类型。
     */
    public enum PsiType {
        /**
         * HFH99椭圆曲线方案
         */
        HFH99_ECC,
        /**
         * HFH99字节椭圆曲线方案
         */
        HFH99_BYTE_ECC,
        /**
         * KKRT16方案
         */
        KKRT16,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static PsiServer createServer(Rpc serverRpc, Party clientParty, PsiConfig config) {
        PsiType type = config.getPtoType();
        switch (type) {
            case HFH99_BYTE_ECC:
            case HFH99_ECC:
            case KKRT16:
            default:
                throw new IllegalArgumentException("Invalid " + PsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 构建客户端。
     *
     * @param clientRpc   客户端通信接口。
     * @param serverParty 服务端信息。
     * @param config      配置项。
     * @return 客户端。
     */
    public static PsiClient createClient(Rpc clientRpc, Party serverParty, PsiConfig config) {
        PsiType type = config.getPtoType();
        switch (type) {
            case HFH99_BYTE_ECC:
            case HFH99_ECC:
            case KKRT16:
            default:
                throw new IllegalArgumentException("Invalid " + PsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
