package edu.alibaba.mpc4j.dp.service.heavyhitter;

import edu.alibaba.mpc4j.dp.service.heavyhitter.config.FoHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.fo.FoHhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.fo.FoHhLdpServer;
import edu.alibaba.mpc4j.dp.service.heavyhitter.hg.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Heavy Hitter LDP Factory.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class HhLdpFactory {
    /**
     * the empty item prefix ⊥
     */
    public static final String BOT_PREFIX = "⊥_";
    /**
     * the default charset
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private HhLdpFactory() {
        // empty
    }

    public enum HhLdpType {
        /**
         * frequency oracle
         */
        FO,
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
     * Create an instance of Heavy Hitter LDP server.
     *
     * @param config the config.
     * @return an instance of Heavy Hitter LDP server.
     */
    public static HhLdpServer createServer(HhLdpConfig config) {
        HhLdpType type = config.getType();
        switch (type) {
            case FO:
                FoHhLdpConfig foConfig = new FoHhLdpConfig.Builder(config).build();
                return new FoHhLdpServer(foConfig);
            case BASIC_HG:
                HgHhLdpConfig basicConfig = new HgHhLdpConfig.Builder(config).build();
                return new BasicHgHhLdpServer(basicConfig);
            case ADVAN_HG:
                HhgHhLdpConfig advConfig = new HhgHhLdpConfig.Builder(config).build();
                return new AdvHhgHhLdpServer(advConfig);
            case RELAX_HG:
                HhgHhLdpConfig relaxConfig = new HhgHhLdpConfig.Builder(config).build();
                return new RelaxHhgHhLdpServer(relaxConfig);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Heavy Hitter LDP client.
     *
     * @param config the config.
     * @return an instance of Heavy Hitter LDP client.
     */
    public static HhLdpClient createClient(HhLdpConfig config) {
        HhLdpType type = config.getType();
        switch (type) {
            case FO:
                FoHhLdpConfig foConfig = new FoHhLdpConfig.Builder(config).build();
                return new FoHhLdpClient(foConfig);
            case BASIC_HG:
                HgHhLdpConfig basicClientConfig = new HgHhLdpConfig.Builder(config).build();
                return new BasicHgHhLdpClient(basicClientConfig);
            case ADVAN_HG:
                HhgHhLdpConfig advClientConfig = new HhgHhLdpConfig.Builder(config).build();
                return new AdvHhgHhLdpClient(advClientConfig);
            case RELAX_HG:
                HhgHhLdpConfig relaxClientConfig = new HhgHhLdpConfig.Builder(config).build();
                return new RelaxHhgHhLdpClient(relaxClientConfig);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Heavy Hitter LDP server based on Frequency Oracle.
     *
     * @param config the config.
     * @return an instance of Heavy Hitter LDP server based on Frequency Oracle.
     */
    public static HhLdpServer createFoServer(FoHhLdpConfig config) {
        return new FoHhLdpServer(config);
    }

    /**
     * Create an instance of Heavy Hitter LDP client based on Frequency Oracle.
     *
     * @param config the config.
     * @return an instance of Heavy Hitter LDP client based on Frequency Oracle.
     */
    public static HhLdpClient createFoClient(FoHhLdpConfig config) {
        return new FoHhLdpClient(config);
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter LDP server.
     *
     * @param config the config.
     * @return an instance of HeavyGuardian-based Heavy Hitter LDP server.
     */
    public static HgHhLdpServer createHgServer(HgHhLdpConfig config) {
        HhLdpType type = config.getType();
        switch (type) {
            case BASIC_HG:
                return new BasicHgHhLdpServer(config);
            case ADVAN_HG:
                HhgHhLdpConfig advConfig = new HhgHhLdpConfig.Builder(config).build();
                return new AdvHhgHhLdpServer(advConfig);
            case RELAX_HG:
                HhgHhLdpConfig relaxConfig = new HhgHhLdpConfig.Builder(config).build();
                return new RelaxHhgHhLdpServer(relaxConfig);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter LDP client.
     *
     * @param config the config.
     * @return an instance of HeavyGuardian-based Heavy Hitter LDP client.
     */
    public static HgHhLdpClient createHgClient(HgHhLdpConfig config) {
        HhLdpType type = config.getType();
        switch (type) {
            case BASIC_HG:
                return new BasicHgHhLdpClient(config);
            case ADVAN_HG:
                HhgHhLdpConfig advClientConfig = new HhgHhLdpConfig.Builder(config).build();
                return new AdvHhgHhLdpClient(advClientConfig);
            case RELAX_HG:
                HhgHhLdpConfig relaxClientConfig = new HhgHhLdpConfig.Builder(config).build();
                return new RelaxHhgHhLdpClient(relaxClientConfig);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter LDP server.
     *
     * @param config the config.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter LDP server.
     */
    public static HhgHhLdpServer createHhgServer(HhgHhLdpConfig config) {
        HhLdpType type = config.getType();
        switch (type) {
            case ADVAN_HG:
                return new AdvHhgHhLdpServer(config);
            case RELAX_HG:
                return new RelaxHhgHhLdpServer(config);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter LDP client.
     *
     * @param config the config.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter LDP client.
     */
    public static HhgHhLdpClient createHhgClient(HhgHhLdpConfig config) {
        HhLdpType type = config.getType();
        switch (type) {
            case ADVAN_HG:
                return new AdvHhgHhLdpClient(config);
            case RELAX_HG:
                return new RelaxHhgHhLdpClient(config);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }
}
