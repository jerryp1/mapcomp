package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.XPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.XPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.XPirServer;

/**
 * 索引PIR协议工厂。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class IndexPirFactory {
    /**
     * 私有构造函数
     */
    private IndexPirFactory() {
        // empty
    }
    /**
     * 索引PIR协议类型。
     */
    public enum IndexPirType {
        /**
         * XPIR
         */
        XPIR,
        // SEAL_PIR,
        // ONION_PIR,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static IndexPirServer createServer(Rpc serverRpc, Party clientParty, IndexPirConfig config) {
        IndexPirType type = config.getProType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case XPIR:
                return new XPirServer(serverRpc, clientParty, (XPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + IndexPirType.class.getSimpleName() + ": " + type.name());
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
    public static IndexPirClient createClient(Rpc clientRpc, Party serverParty, IndexPirConfig config) {
        IndexPirType type = config.getProType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case XPIR:
                return new XPirClient(clientRpc, serverParty, (XPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + IndexPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
