package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.cm20.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.czz22.Czz22PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.czz22.Czz22PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.czz22.Czz22PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.gmr21.Gmr21PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.gmr21.Gmr21PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.gmr21.Gmr21PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.prty20.Prty20PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.prty20.Prty20PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.prty20.Prty20PsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.ra17.Ra17PsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.ra17.Ra17PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.ra17.Ra17PsiServer;

/**
 * PSI协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class PsiFactory implements PtoFactory {
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
        /**
         * CM20方案
         */
        CM20,
        /**
         * CZZ22方案
         */
        CZZ22,
        /**
         * GMR21方案
         */
        GMR21,
        /**
         * PRTY19 fast方案
         */
        PRTY19FAST,
        /**
         * PRTY19 低通信量方案
         */
        PRTY19LOW,
        /**
         * 使用PaXoS的PSI方案
         */
        PRTY20,
        /**
         * PSZ14 使用garbled BF的方案
         */
        PSZ14GBF,
        /**
         * PSZ14方案
         */
        PSZ14,
        /**
         * RA17方案
         */
        RA17,
        /**
         * RT21方案
         */
        RT21,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static <X> PsiServer<X> createServer(Rpc serverRpc, Party clientParty, PsiConfig config) {
        PsiType type = config.getPtoType();
        switch (type) {
            case HFH99_ECC:
                return new Hfh99EccPsiServer<>(serverRpc, clientParty, (Hfh99EccPsiConfig) config);
            case HFH99_BYTE_ECC:
                return new Hfh99ByteEccPsiServer<>(serverRpc, clientParty, (Hfh99ByteEccPsiConfig) config);
            case KKRT16:
                return new Kkrt16PsiServer<>(serverRpc, clientParty, (Kkrt16PsiConfig) config);
            case CM20:
                return new Cm20PsiServer<>(serverRpc, clientParty, (Cm20PsiConfig) config);
            case CZZ22:
                return new Czz22PsiServer<>(serverRpc, clientParty, (Czz22PsiConfig) config);
            case GMR21:
                return new Gmr21PsiServer<>(serverRpc, clientParty, (Gmr21PsiConfig) config);
            case PRTY20:
                return new Prty20PsiServer<>(serverRpc, clientParty, (Prty20PsiConfig) config);
            case RA17:
                return new Ra17PsiServer<>(serverRpc, clientParty, (Ra17PsiConfig) config);
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
    public static <X> PsiClient<X> createClient(Rpc clientRpc, Party serverParty, PsiConfig config) {
        PsiType type = config.getPtoType();
        switch (type) {
            case HFH99_ECC:
                return new Hfh99EccPsiClient<>(clientRpc, serverParty, (Hfh99EccPsiConfig) config);
            case HFH99_BYTE_ECC:
                return new Hfh99ByteEccPsiClient<>(clientRpc, serverParty, (Hfh99ByteEccPsiConfig) config);
            case KKRT16:
                return new Kkrt16PsiClient<>(clientRpc, serverParty, (Kkrt16PsiConfig) config);
            case CM20:
                return new Cm20PsiClient<>(clientRpc, serverParty, (Cm20PsiConfig) config);
            case CZZ22:
                return new Czz22PsiClient<>(clientRpc, serverParty, (Czz22PsiConfig) config);
            case GMR21:
                return new Gmr21PsiClient<>(clientRpc, serverParty, (Gmr21PsiConfig) config);
            case PRTY20:
                return new Prty20PsiClient<>(clientRpc, serverParty, (Prty20PsiConfig) config);
            case RA17:
                return new Ra17PsiClient<>(clientRpc, serverParty, (Ra17PsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
