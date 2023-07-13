package edu.alibaba.mpc4j.s2pc.pir.index.batch;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.cuckoohash.CuckooHashBatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.cuckoohash.CuckooHashBatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.cuckoohash.CuckooHashBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir.Lpzl24BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir.Lpzl24BatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir.Lpzl24BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.simplepir.CuckooHashBatchSimplePirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.simplepir.CuckooHashBatchSimplePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.simplepir.CuckooHashBatchSimplePirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirServer;

/**
 * batch index PIR factory.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class BatchIndexPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private BatchIndexPirFactory() {
        // empty
    }

    /**
     * batch index PIR type
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
        /**
         * XPIR
         */
        XPIR,
        /**
         * SealPIR
         */
        SEAL_PIR,
        /**
         * OnionPIR
         */
        ONION_PIR,
        /**
         * FastPIR
         */
        FAST_PIR,
        /**
         * Vectorized PIR
         */
        VECTORIZED_PIR,
        /**
         * Mul PIR
         */
        MUL_PIR,
        /**
         * simple PIR
         */
        SIMPLE_PIR,
        /**
         * Constant Weight PIR
         */
        CONSTANT_WEIGHT_PIR,
    }

    /**
     * create server.
     *
     * @param serverRpc   server rpc.
     * @param clientParty client party.
     * @param config      config.
     * @return server.
     */
    public static BatchIndexPirServer createServer(Rpc serverRpc, Party clientParty, BatchIndexPirConfig config) {
        BatchIndexPirType type = config.getPtoType();
        switch (type) {
            case PSI_PIR:
                return new Lpzl24BatchIndexPirServer(serverRpc, clientParty, (Lpzl24BatchIndexPirConfig) config);
            case VECTORIZED_BATCH_PIR:
            case VECTORIZED_PIR:
                return new Mr23BatchIndexPirServer(serverRpc, clientParty, (Mr23BatchIndexPirConfig) config);
            case XPIR:
            case SEAL_PIR:
            case ONION_PIR:
            case FAST_PIR:
            case CONSTANT_WEIGHT_PIR:
            case MUL_PIR:
                return new CuckooHashBatchIndexPirServer(serverRpc, clientParty, (CuckooHashBatchIndexPirConfig) config);
            case SIMPLE_PIR:
                return new CuckooHashBatchSimplePirServer(serverRpc, clientParty, (CuckooHashBatchSimplePirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BatchIndexPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create client.
     *
     * @param clientRpc   client rpc.
     * @param serverParty server party.
     * @param config      config.
     * @return client.
     */
    public static BatchIndexPirClient createClient(Rpc clientRpc, Party serverParty, BatchIndexPirConfig config) {
        BatchIndexPirType type = config.getPtoType();
        switch (type) {
            case PSI_PIR:
                return new Lpzl24BatchIndexPirClient(clientRpc, serverParty, (Lpzl24BatchIndexPirConfig) config);
            case VECTORIZED_BATCH_PIR:
            case VECTORIZED_PIR:
                return new Mr23BatchIndexPirClient(clientRpc, serverParty, (Mr23BatchIndexPirConfig) config);
            case XPIR:
            case SEAL_PIR:
            case ONION_PIR:
            case FAST_PIR:
            case CONSTANT_WEIGHT_PIR:
            case MUL_PIR:
                return new CuckooHashBatchIndexPirClient(clientRpc, serverParty, (CuckooHashBatchIndexPirConfig) config);
            case SIMPLE_PIR:
                return new CuckooHashBatchSimplePirClient(clientRpc, serverParty, (CuckooHashBatchSimplePirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + BatchIndexPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
