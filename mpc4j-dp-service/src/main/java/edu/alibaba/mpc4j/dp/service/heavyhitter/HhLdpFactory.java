package edu.alibaba.mpc4j.dp.service.heavyhitter;

import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.fo.DeFoHhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.fo.DeFoHhLdpServer;
import edu.alibaba.mpc4j.dp.service.heavyhitter.hg.*;

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

    private HhLdpFactory() {
        // empty
    }

    public enum HhLdpType {
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
     * @param hhLdpConfig the config.
     * @return an instance of Heavy Hitter server with Local Differential Privacy.
     */
    public static HhLdpServer createServer(HhLdpConfig hhLdpConfig) {
        HhLdpType type = hhLdpConfig.getType();
        switch (type) {
            case DE_FO:
                return new DeFoHhLdpServer(hhLdpConfig);
            case BASIC_HG:
                HgHhLdpConfig basicConfig = new HgHhLdpConfig.Builder(hhLdpConfig).build();
                return new BasicHgHhLdpServer(basicConfig);
            case ADVAN_HG:
                HhgHhLdpConfig advConfig = new HhgHhLdpConfig.Builder(hhLdpConfig).build();
                return new AdvHhgHhLdpServer(advConfig);
            case RELAX_HG:
                HhgHhLdpConfig relaxConfig = new HhgHhLdpConfig.Builder(hhLdpConfig).build();
                return new RelaxHhgHhLdpServer(relaxConfig);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Heavy Hitter client with Local Differential Privacy.
     *
     * @param hhLdpConfig the config.
     * @return an instance of Heavy Hitter client with Local Differential Privacy.
     */
    public static HhLdpClient createClient(HhLdpConfig hhLdpConfig) {
        HhLdpType type = hhLdpConfig.getType();
        switch (type) {
            case DE_FO:
                return new DeFoHhLdpClient(hhLdpConfig);
            case BASIC_HG:
                HgHhLdpConfig basicClientConfig = new HgHhLdpConfig.Builder(hhLdpConfig).build();
                return new BasicHgHhLdpClient(basicClientConfig);
            case ADVAN_HG:
                HhgHhLdpConfig advClientConfig = new HhgHhLdpConfig.Builder(hhLdpConfig).build();
                return new AdvHhgHhLdpClient(advClientConfig);
            case RELAX_HG:
                HhgHhLdpConfig relaxClientConfig = new HhgHhLdpConfig.Builder(hhLdpConfig).build();
                return new RelaxHhgHhLdpClient(relaxClientConfig);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter server with Local Differential Privacy.
     *
     * @param hgHhLdpConfig the config.
     * @return an instance of HeavyGuardian-based Heavy Hitter server with Local Differential Privacy.
     */
    public static HgHhLdpServer createHgServer(HgHhLdpConfig hgHhLdpConfig) {
        HhLdpType type = hgHhLdpConfig.getType();
        switch (type) {
            case BASIC_HG:
                return new BasicHgHhLdpServer(hgHhLdpConfig);
            case ADVAN_HG:
                HhgHhLdpConfig advConfig = new HhgHhLdpConfig.Builder(hgHhLdpConfig).build();
                return new AdvHhgHhLdpServer(advConfig);
            case RELAX_HG:
                HhgHhLdpConfig relaxConfig = new HhgHhLdpConfig.Builder(hgHhLdpConfig).build();
                return new RelaxHhgHhLdpServer(relaxConfig);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter client with Local Differential Privacy.
     *
     * @param hgHhLdpConfig the config.
     * @return an instance of HeavyGuardian-based Heavy Hitter client with Local Differential Privacy.
     */
    public static HgHhLdpClient createHgClient(HgHhLdpConfig hgHhLdpConfig) {
        HhLdpType type = hgHhLdpConfig.getType();
        switch (type) {
            case BASIC_HG:
                return new BasicHgHhLdpClient(hgHhLdpConfig);
            case ADVAN_HG:
                HhgHhLdpConfig advClientConfig = new HhgHhLdpConfig.Builder(hgHhLdpConfig).build();
                return new AdvHhgHhLdpClient(advClientConfig);
            case RELAX_HG:
                HhgHhLdpConfig relaxClientConfig = new HhgHhLdpConfig.Builder(hgHhLdpConfig).build();
                return new RelaxHhgHhLdpClient(relaxClientConfig);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter server with Local Differential Privacy.
     *
     * @param hhgHhLdpConfig the config.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter server with Local Differential Privacy.
     */
    public static HhgHhLdpServer createHhgServer(HhgHhLdpConfig hhgHhLdpConfig) {
        HhLdpType type = hhgHhLdpConfig.getType();
        switch (type) {
            case ADVAN_HG:
                return new AdvHhgHhLdpServer(hhgHhLdpConfig);
            case RELAX_HG:
                return new RelaxHhgHhLdpServer(hhgHhLdpConfig);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter client with Local Differential Privacy.
     *
     * @param hhgHhLdpConfig the config.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter client with Local Differential Privacy.
     */
    public static HhgHhLdpClient createHhgClient(HhgHhLdpConfig hhgHhLdpConfig) {
        HhLdpType type = hhgHhLdpConfig.getType();
        switch (type) {
            case ADVAN_HG:
                return new AdvHhgHhLdpClient(hhgHhLdpConfig);
            case RELAX_HG:
                return new RelaxHhgHhLdpClient(hhgHhLdpConfig);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }
}
