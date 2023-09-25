package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleKeywordCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleKeywordCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.alpr21.Alpr21SingleKeywordCpPirServer;

/**
 * Single Keyword Client-specific Preprocessing PIR factory.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public class SingleKeywordCpPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SingleKeywordCpPirFactory() {
        // empty
    }

    /**
     * Single Keyword Client-specific Preprocessing PIR type
     */
    public enum SingleKeywordCpPirType {
        /**
         * ALPR21
         */
        ALPR21,
        /**
         * LLP23
         */
        LLP23_STREAM_PIR,
    }

    /**
     * create a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static <T> SingleKeywordCpPirServer<T> createServer(Rpc serverRpc, Party clientParty, SingleKeywordCpPirConfig config) {
        SingleKeywordCpPirType type = config.getProType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case ALPR21:
                return new Alpr21SingleKeywordCpPirServer<>(serverRpc, clientParty, (Alpr21SingleKeywordCpPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleKeywordCpPirType.class.getSimpleName() + ": " + type.name()
                );
        }
    }

    /**
     * create a client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param config      config.
     * @return a client.
     */
    public static <T> SingleKeywordCpPirClient<T> createClient(Rpc clientRpc, Party serverParty, SingleKeywordCpPirConfig config) {
        SingleKeywordCpPirType type = config.getProType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case ALPR21:
                return new Alpr21SingleKeywordCpPirClient<>(clientRpc, serverParty, (Alpr21SingleKeywordCpPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleKeywordCpPirType.class.getSimpleName() + ": " + type.name()
                );
        }
    }
}
