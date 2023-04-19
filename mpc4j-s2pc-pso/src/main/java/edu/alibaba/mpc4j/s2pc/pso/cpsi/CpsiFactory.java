package edu.alibaba.mpc4j.s2pc.pso.cpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19.Psty19CpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19.Psty19CpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.psty19.Psty19CpsiServer;

/**
 * Circuit PSI factory.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class CpsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private CpsiFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum CpsiType {
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
    public static CpsiServer createServer(Rpc serverRpc, Party clientParty, CpsiConfig config) {
        CpsiType type = config.getPtoType();
        switch (type) {
            case PSTY19:
                return new Psty19CpsiServer(serverRpc, clientParty, (Psty19CpsiConfig) config);
            case RS21:
            case CGS22:
            default:
                throw new IllegalArgumentException("Invalid " + CpsiType.class.getSimpleName() + ": " + type.name());
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
    public static CpsiClient createClient(Rpc clientRpc, Party serverParty, CpsiConfig config) {
        CpsiType type = config.getPtoType();
        switch (type) {
            case PSTY19:
                return new Psty19CpsiClient(clientRpc, serverParty, (Psty19CpsiConfig) config);
            case RS21:
            case CGS22:
            default:
                throw new IllegalArgumentException("Invalid " + CpsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
