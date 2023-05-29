package edu.alibaba.mpc4j.s2pc.pir.index.single;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirServer;

/**
 * Single Index PIR factory.
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class SingleIndexPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SingleIndexPirFactory() {
        // empty
    }

    /**
     * single index PIR type
     */
    public enum SingleIndexPirType {
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
    }

    /**
     * create single index PIR server.
     *
     * @param serverRpc   server rpc.
     * @param clientParty client party.
     * @param config      config.
     * @return single index PIR server.
     */
    public static SingleIndexPirServer createServer(Rpc serverRpc, Party clientParty, SingleIndexPirConfig config) {
        SingleIndexPirType type = config.getProType();
        switch (type) {
            case XPIR:
                return new Mbfk16SingleIndexPirServer(serverRpc, clientParty, (Mbfk16SingleIndexPirConfig) config);
            case SEAL_PIR:
                return new Acls18SingleIndexPirServer(serverRpc, clientParty, (Acls18SingleIndexPirConfig) config);
            case ONION_PIR:
                return new Mcr21SingleIndexPirServer(serverRpc, clientParty, (Mcr21SingleIndexPirConfig) config);
            case FAST_PIR:
                return new Ayaa21SingleIndexPirServer(serverRpc, clientParty, (Ayaa21SingleIndexPirConfig) config);
            case VECTORIZED_PIR:
                return new Mr23SingleIndexPirServer(serverRpc, clientParty, (Mr23SingleIndexPirConfig) config);
            case MUL_PIR:
                return new Alpr21SingleIndexPirServer(serverRpc, clientParty, (Alpr21SingleIndexPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleIndexPirType.class.getSimpleName() + ": " + type.name()
                );
        }
    }

    /**
     * create single index PIR client.
     *
     * @param clientRpc   client rpc.
     * @param serverParty server party.
     * @param config      config.
     * @return single index PIR client.
     */
    public static SingleIndexPirClient createClient(Rpc clientRpc, Party serverParty, SingleIndexPirConfig config) {
        SingleIndexPirType type = config.getProType();
        switch (type) {
            case XPIR:
                return new Mbfk16SingleIndexPirClient(clientRpc, serverParty, (Mbfk16SingleIndexPirConfig) config);
            case SEAL_PIR:
                return new Acls18SingleIndexPirClient(clientRpc, serverParty, (Acls18SingleIndexPirConfig) config);
            case ONION_PIR:
                return new Mcr21SingleIndexPirClient(clientRpc, serverParty, (Mcr21SingleIndexPirConfig) config);
            case FAST_PIR:
                return new Ayaa21SingleIndexPirClient(clientRpc, serverParty, (Ayaa21SingleIndexPirConfig) config);
            case VECTORIZED_PIR:
                return new Mr23SingleIndexPirClient(clientRpc, serverParty, (Mr23SingleIndexPirConfig) config);
            case MUL_PIR:
                return new Alpr21SingleIndexPirClient(clientRpc, serverParty, (Alpr21SingleIndexPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleIndexPirType.class.getSimpleName() + ": " + type.name()
                );
        }
    }
}