package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.config.*;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.fo.DeFoLdpHhClient;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.hg.*;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.fo.DeFoLdpHhServer;

/**
 * Heavy Hitter with Local Differential Privacy Factory.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class LdpHhFactory {
    /**
     * the empty item prefix ⊥
     */
    public static final String BOT_PREFIX = "⊥_";

    private LdpHhFactory() {
        // empty
    }

    public enum LdpHhType {
        /**
         * direct encoding
         */
        DE_FO,
        /**
         * basic HeavyGuardian
         */
        BASIC_HG,
        /**
         * Advanced HeavyGuardian
         */
        ADVAN_HG,
        /**
         * Related HeavyGuardian
         */
        RELAX_HG,
    }

    /**
     * Create an instance of Heavy Hitter server with Local Differential Privacy.
     *
     * @param serverConfig the server config.
     * @return an instance of Heavy Hitter server with Local Differential Privacy.
     */
    public static LdpHhServer createServer(LdpHhServerConfig serverConfig) {
        LdpHhType type = serverConfig.getType();
        switch (type) {
            case DE_FO:
                return new DeFoLdpHhServer(serverConfig);
            case BASIC_HG:
                HgLdpHhServerConfig basicServerConfig = new HgLdpHhServerConfig.Builder(serverConfig).build();
                return new BasicHgLdpHhServer(basicServerConfig);
            case ADVAN_HG:
                HhgLdpHhServerConfig advServerConfig = new HhgLdpHhServerConfig.Builder(serverConfig).build();
                return new AdvHhgLdpHhServer(advServerConfig);
            case RELAX_HG:
                HhgLdpHhServerConfig relaxServerConfig = new HhgLdpHhServerConfig.Builder(serverConfig).build();
                return new RelaxHhgLdpHhServer(relaxServerConfig);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHhType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Heavy Hitter client with Local Differential Privacy.
     *
     * @param clientConfig the client config.
     * @return an instance of Heavy Hitter client with Local Differential Privacy.
     */
    public static LdpHhClient createClient(LdpHhClientConfig clientConfig) {
        LdpHhType type = clientConfig.getType();
        switch (type) {
            case DE_FO:
                return new DeFoLdpHhClient(clientConfig);
            case BASIC_HG:
                HgLdpHhClientConfig basicClientConfig = new HgLdpHhClientConfig.Builder(clientConfig).build();
                return new BasicHgLdpHhClient(basicClientConfig);
            case ADVAN_HG:
                HhgLdpHhClientConfig advClientConfig = new HhgLdpHhClientConfig.Builder(clientConfig).build();
                return new AdvHhgLdpHhClient(advClientConfig);
            case RELAX_HG:
                HhgLdpHhClientConfig relaxClientConfig = new HhgLdpHhClientConfig.Builder(clientConfig).build();
                return new RelaxHhgLdpHhClient(relaxClientConfig);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHhType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter server with Local Differential Privacy.
     *
     * @param serverConfig the server config.
     * @return an instance of HeavyGuardian-based Heavy Hitter server with Local Differential Privacy.
     */
    public static HgLdpHhServer createHgServer(HgLdpHhServerConfig serverConfig) {
        LdpHhType type = serverConfig.getType();
        switch (type) {
            case BASIC_HG:
                return new BasicHgLdpHhServer(serverConfig);
            case ADVAN_HG:
                HhgLdpHhServerConfig advServerConfig = new HhgLdpHhServerConfig.Builder(serverConfig).build();
                return new AdvHhgLdpHhServer(advServerConfig);
            case RELAX_HG:
                HhgLdpHhServerConfig relaxServerConfig = new HhgLdpHhServerConfig.Builder(serverConfig).build();
                return new RelaxHhgLdpHhServer(relaxServerConfig);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHhType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter client with Local Differential Privacy.
     *
     * @param clientConfig the clientConfig.
     * @return an instance of HeavyGuardian-based Heavy Hitter client with Local Differential Privacy.
     */
    public static HgLdpHhClient createHgClient(HgLdpHhClientConfig clientConfig) {
        LdpHhType type = clientConfig.getType();
        switch (type) {
            case BASIC_HG:
                return new BasicHgLdpHhClient(clientConfig);
            case ADVAN_HG:
                HhgLdpHhClientConfig advClientConfig = new HhgLdpHhClientConfig.Builder(clientConfig).build();
                return new AdvHhgLdpHhClient(advClientConfig);
            case RELAX_HG:
                HhgLdpHhClientConfig relaxClientConfig = new HhgLdpHhClientConfig.Builder(clientConfig).build();
                return new RelaxHhgLdpHhClient(relaxClientConfig);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHhType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter server with Local Differential Privacy.
     *
     * @param serverConfig the server config.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter server with Local Differential Privacy.
     */
    public static HhgLdpHhServer createHhgServer(HhgLdpHhServerConfig serverConfig) {
        LdpHhType type = serverConfig.getType();
        switch (type) {
            case ADVAN_HG:
                return new AdvHhgLdpHhServer(serverConfig);
            case RELAX_HG:
                return new RelaxHhgLdpHhServer(serverConfig);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHhType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter client with Local Differential Privacy.
     *
     * @param clientConfig the client config.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter client with Local Differential Privacy.
     */
    public static HhgLdpHhClient createHhgClient(HhgLdpHhClientConfig clientConfig) {
        LdpHhType type = clientConfig.getType();
        switch (type) {
            case ADVAN_HG:
                return new AdvHhgLdpHhClient(clientConfig);
            case RELAX_HG:
                return new RelaxHhgLdpHhClient(clientConfig);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHhType.class.getSimpleName() + ": " + type);
        }
    }
}
