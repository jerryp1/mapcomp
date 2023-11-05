package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.psty19.Psty19PlpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.psty19.Psty19PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.psty19.Psty19PlpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21.Rs21PlpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21.Rs21PlpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.rs21.Rs21PlpsiServer;

/**
 * payload-circuit PSI factory, where server encodes payload into circuit
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class PlpsiFactory {
    /**
     * private constructor.
     */
    private PlpsiFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum PlpsiType {
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
    public static <X, T> PlpsiServer<X, T> createServer(Rpc serverRpc, Party clientParty, PlpsiConfig config) {
        PlpsiType type = config.getPtoType();
        switch (type) {
            case RS21:
                return new Rs21PlpsiServer<>(serverRpc, clientParty, (Rs21PlpsiConfig) config);
            case PSTY19:
                return new Psty19PlpsiServer<>(serverRpc, clientParty, (Psty19PlpsiConfig) config);
            case CGS22:
            default:
                throw new IllegalArgumentException("Invalid " + PlpsiType.class.getSimpleName() + ": " + type.name());
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
    public static <X> PlpsiClient<X> createClient(Rpc clientRpc, Party serverParty, PlpsiConfig config) {
        PlpsiType type = config.getPtoType();
        switch (type) {
            case RS21:
                return new Rs21PlpsiClient<>(clientRpc, serverParty, (Rs21PlpsiConfig) config);
            case PSTY19:
                return new Psty19PlpsiClient<>(clientRpc, serverParty, (Psty19PlpsiConfig) config);
            case CGS22:
            default:
                throw new IllegalArgumentException("Invalid " + PlpsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static PlpsiConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case TRUSTED_DEALER:
            case SEMI_HONEST:
                return new Rs21PlpsiConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
