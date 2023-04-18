package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UnbalancedCpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UnbalancedCpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UnbalancedCpsiServer;

/**
 * Unbalanced Circuit PSI factory.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class UnbalancedCpsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private UnbalancedCpsiFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum UnbalancedCpsiType {
        /**
         * PSTY19 circuit PSI
         */
        PSTY19,
        /**
         * CGS22 circuit PSI
         */
        CGS22,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static <T> UnbalancedCpsiServer<T> createServer(Rpc serverRpc, Party clientParty, UnbalancedCpsiConfig config) {
        UnbalancedCpsiFactory.UnbalancedCpsiType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case PSTY19:
                return new Psty19UnbalancedCpsiServer<>(serverRpc, clientParty, config);
            default:
                throw new IllegalArgumentException("Invalid " + UnbalancedCpsiFactory.UnbalancedCpsiType.class.getSimpleName() + ": " + type.name());
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
    public static <T> UnbalancedCpsiClient<T> createClient(Rpc clientRpc, Party serverParty, UnbalancedCpsiConfig config) {
        UnbalancedCpsiFactory.UnbalancedCpsiType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case PSTY19:
                return new Psty19UnbalancedCpsiClient<>(clientRpc, serverParty, config);
            default:
                throw new IllegalArgumentException("Invalid " + UnbalancedCpsiFactory.UnbalancedCpsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
