package edu.alibaba.mpc4j.s2pc.pir.batchindex;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir.Lpzg24BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir.Lpzg24BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir.Lpzg24BatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirServer;

/**
 * 批量索引PIR协议工厂。
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class BatchIndexPirFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private BatchIndexPirFactory() {
        // empty
    }

    /**
     * 批量索引PIR协议类型。
     */
    public enum BatchIndexPirType {
        /**
         * PSI_PIR
         */
        PSI_PIR,
        /**
         * Vectorized Batch PIR
         */
        VECTORIZED_BATCH_PIR,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static BatchIndexPirServer createServer(Rpc serverRpc, Party clientParty, BatchIndexPirConfig config) {
        BatchIndexPirType type = config.getProType();
        switch (type) {
            case PSI_PIR:
                return new Lpzg24BatchIndexPirServer(serverRpc, clientParty, (Lpzg24BatchIndexPirConfig) config);
            case VECTORIZED_BATCH_PIR:
                return new Mr23BatchIndexPirServer(serverRpc, clientParty, (Mr23BatchIndexPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BatchIndexPirType.class.getSimpleName() + ": " + type.name());
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
    public static BatchIndexPirClient createClient(Rpc clientRpc, Party serverParty, BatchIndexPirConfig config) {
        BatchIndexPirType type = config.getProType();
        switch (type) {
            case PSI_PIR:
                return new Lpzg24BatchIndexPirClient(clientRpc, serverParty, (Lpzg24BatchIndexPirConfig) config);
            case VECTORIZED_BATCH_PIR:
                return new Mr23BatchIndexPirClient(clientRpc, serverParty, (Mr23BatchIndexPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BatchIndexPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
