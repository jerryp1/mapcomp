package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.php24.Php24PmapClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.php24.Php24PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.php24.Php24PmapServer;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.pidbased.PidBasedPmapClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.pidbased.PidBasedPmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.pidbased.PidBasedPmapServer;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased.PsiBasedPmapClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased.PsiBasedPmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.psibased.PsiBasedPmapServer;

/**
 * PMAP factory
 *
 * @author Feng Han
 * @date 2022/01/19
 */
public class PmapFactory {
    /**
     * private constructor
     */
    private PmapFactory() {
        // empty
    }

    /**
     * pmap type
     */
    public enum PmapPtoType {
        /**
         * PHP24
         */
        PHP24,
        /**
         * based on PID
         */
        PID_BASED,
        /**
         * based on PSI
         */
        PSI_BASED,
    }

    /**
     * Creates a Payable PSI server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a PSI server.
     */
    public static <T> PmapServer<T> createServer(Rpc serverRpc, Party clientParty, PmapConfig config) {
        PmapPtoType type = config.getPtoType();
        switch (type){
            case PHP24:
                return new Php24PmapServer<>(serverRpc, clientParty, (Php24PmapConfig) config);
            case PID_BASED:
                return new PidBasedPmapServer<>(serverRpc, clientParty, (PidBasedPmapConfig) config);
            case PSI_BASED:
                return new PsiBasedPmapServer<>(serverRpc, clientParty, (PsiBasedPmapConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PmapPtoType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a Payable PSI client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param config      config.
     * @return a client.
     */
    public static <T> PmapClient<T> createClient(Rpc clientRpc, Party serverParty, PmapConfig config) {
        PmapPtoType type = config.getPtoType();
        switch (type){
            case PHP24:
                return new Php24PmapClient<>(clientRpc, serverParty, (Php24PmapConfig) config);
            case PID_BASED:
                return new PidBasedPmapClient<>(clientRpc, serverParty, (PidBasedPmapConfig) config);
            case PSI_BASED:
                return new PsiBasedPmapClient<>(clientRpc, serverParty, (PsiBasedPmapConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PmapPtoType.class.getSimpleName() + ": " + type.name());
        }
    }

    public static PmapConfig createDefaultConfig(boolean silent){
        return new Php24PmapConfig.Builder(silent).build();
    }
}
