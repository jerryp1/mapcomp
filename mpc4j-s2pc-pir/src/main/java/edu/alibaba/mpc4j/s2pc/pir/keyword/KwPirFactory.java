package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirServer;

/**
 * Keyword PIR factory.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class KwPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private KwPirFactory() {
        // empty
    }

    /**
     * keyword PIR type
     */
    public enum KwPirType {
        /**
         * CMG21
         */
        CMG21,
        /**
         * AAAG22
         */
        AAAG22,
    }

    /**
     * create keyword PIR server.
     *
     * @param serverRpc   server rpc.
     * @param clientParty client party.
     * @param config      config.
     * @return keyword PIR server.
     */
    public static <T> KwPirServer<T> createServer(Rpc serverRpc, Party clientParty, KwPirConfig config) {
        KwPirType type = config.getProType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CMG21:
                return new Cmg21KwPirServer<>(serverRpc, clientParty, (Cmg21KwPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + KwPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create keyword PIR client.
     *
     * @param clientRpc   client rpc.
     * @param serverParty server party.
     * @param config      config.
     * @return keyword PIR client.
     */
    public static <T> KwPirClient<T> createClient(Rpc clientRpc, Party serverParty, KwPirConfig config) {
        KwPirType type = config.getProType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CMG21:
                return new Cmg21KwPirClient<>(clientRpc, serverParty, (Cmg21KwPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + KwPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create default config.
     *
     * @param securityModel security model.
     * @return default config.
     */
    public static KwPirConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
            case COVERT:
            case MALICIOUS:
                return new Cmg21KwPirConfig.Builder().build();
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name()
                );
        }
    }
}
