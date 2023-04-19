package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19.Psty19ScpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19.Psty19ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19.Psty19ScpsiServer;

/**
 * server-payload circuit PSI factory.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class ScpsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ScpsiFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum ScpsiType {
        /**
         * PSTY19 circuit PSI
         */
        PSTY19,
        /**
         * RS21 circuit PSI
         */
        RS21,
        /**
         * CGS22 circuit PSI
         */
        CGS22,
    }

    /**
     * Creates a server.
     *
     * @param serverRpc   the server RPC.
     * @param clientParty the client party.
     * @param config      the config.
     * @return a server.
     */
    public static ScpsiServer createServer(Rpc serverRpc, Party clientParty, ScpsiConfig config) {
        ScpsiType type = config.getPtoType();
        switch (type) {
            case PSTY19:
                return new Psty19ScpsiServer(serverRpc, clientParty, (Psty19ScpsiConfig) config);
            case RS21:
            case CGS22:
            default:
                throw new IllegalArgumentException("Invalid " + ScpsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a client.
     *
     * @param clientRpc   the client RPC.
     * @param serverParty the server party.
     * @param config      the config.
     * @return a client.
     */
    public static ScpsiClient createClient(Rpc clientRpc, Party serverParty, ScpsiConfig config) {
        ScpsiType type = config.getPtoType();
        switch (type) {
            case PSTY19:
                return new Psty19ScpsiClient(clientRpc, serverParty, (Psty19ScpsiConfig) config);
            case RS21:
            case CGS22:
            default:
                throw new IllegalArgumentException("Invalid " + ScpsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
