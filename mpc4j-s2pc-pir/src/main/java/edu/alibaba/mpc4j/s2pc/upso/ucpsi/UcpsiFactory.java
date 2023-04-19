package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiServer;

/**
 * Unbalanced Circuit PSI factory.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class UcpsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private UcpsiFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum UcpsiType {
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
    public static UcpsiServer createServer(Rpc serverRpc, Party clientParty, UcpsiConfig config) {
        UcpsiType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case PSTY19:
                return new Psty19UcpsiServer(serverRpc, clientParty, config);
            default:
                throw new IllegalArgumentException("Invalid " + UcpsiType.class.getSimpleName() + ": " + type.name());
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
    public static UcpsiClient createClient(Rpc clientRpc, Party serverParty, UcpsiConfig config) {
        UcpsiType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case PSTY19:
                return new Psty19UcpsiClient(clientRpc, serverParty, config);
            default:
                throw new IllegalArgumentException("Invalid " + UcpsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
