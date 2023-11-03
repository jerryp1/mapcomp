package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.hpl24.Hpl24PmapClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.hpl24.Hpl24PmapConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.hpl24.Hpl24PmapServer;

/**
 * PMAP协议工厂。
 *
 * @author Weiran Liu
 * @date 2022/01/19
 */
public class PmapFactory {
    /**
     * 私有构造函数
     */
    private PmapFactory() {
        // empty
    }

    /**
     * PID协议类型。
     */
    public enum PmapType {
        /**
         * HPL24方案
         */
        HPL24,
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
        PmapType type = config.getPtoType();
        if (type == PmapType.HPL24) {
            return new Hpl24PmapServer<>(serverRpc, clientParty, (Hpl24PmapConfig) config);
        }
        throw new IllegalArgumentException("Invalid " + PmapType.class.getSimpleName() + ": " + type.name());
    }

    /**
     * Creates a Payable PSI client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param config      config.
     * @return a client.
     */
    public static <X> PmapClient<X> createClient(Rpc clientRpc, Party serverParty, PmapConfig config) {
        PmapType type = config.getPtoType();
        if (type == PmapType.HPL24) {
            return new Hpl24PmapClient<>(clientRpc, serverParty, (Hpl24PmapConfig) config);
        }
        throw new IllegalArgumentException("Invalid " + PmapType.class.getSimpleName() + ": " + type.name());
    }
}
