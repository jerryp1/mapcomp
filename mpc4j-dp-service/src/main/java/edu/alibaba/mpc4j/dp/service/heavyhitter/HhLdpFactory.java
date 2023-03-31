package edu.alibaba.mpc4j.dp.service.heavyhitter;

import edu.alibaba.mpc4j.dp.service.heavyhitter.config.*;
import edu.alibaba.mpc4j.dp.service.heavyhitter.fo.FoHhLdpClient;
import edu.alibaba.mpc4j.dp.service.heavyhitter.fo.FoHhLdpServer;
import edu.alibaba.mpc4j.dp.service.heavyhitter.hg.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Set;

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
         * Frequency Oracle
         */
        FO,
        /**
         * Basic HeavyGuardian
         */
        BASIC,
        /**
         * Direct HeavyGuardian
         */
        DIRECT,
        /**
         * Advanced HeavyGuardian
         */
        ADV,
        /**
         * Buffer HeavyGuardian
         */
        BUFFER,
    }

    /**
     * Creates an default config.
     *
     * @param type          the type.
     * @param domainSet     the domain set.
     * @param k             the k.
     * @param windowEpsilon the window epsilon.
     * @param windowSize    the window size (w).
     * @return an default config.
     */
    public static HhLdpConfig createDefaultHhLdpConfig(HhLdpType type, Set<String> domainSet,
                                                       int k, double windowEpsilon, int windowSize) {
        switch (type) {
            case FO:
                return new FoHhLdpConfig.Builder(domainSet, k, windowEpsilon, windowSize).build();
            case BASIC:
                return new BasicHgHhLdpConfig.Builder(domainSet, k, windowEpsilon, windowSize).build();
            case DIRECT:
                return new DirectHgHhLdpConfig.Builder(domainSet, k, windowEpsilon, windowSize).build();
            case ADV:
                return new AdvHhgHhLdpConfig.Builder(domainSet, k, windowEpsilon, windowSize).build();
            case BUFFER:
                return new BufferHhgHhLdpConfig.Builder(domainSet, k, windowEpsilon, windowSize).build();
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Creates an default config.
     *
     * @param type          the type.
     * @param domainSet     the domain set.
     * @param k             the k.
     * @param windowEpsilon the window epsilon.
     * @param windowSize    the window size (w).
     * @param w the bucket size.
     * @param lambdaH  λ_h, i.e., the cell num in each bucket.
     * @param hgRandom the randomness used in the HeavyGuardian.
     * @return an default config.
     */
    public static HgHhLdpConfig createDefaultHgHhLdpConfig(HhLdpType type, Set<String> domainSet,
                                                         int k, double windowEpsilon, int windowSize,
                                                         int w, int lambdaH, Random hgRandom) {
        switch (type) {
            case BASIC:
                return new BasicHgHhLdpConfig
                    .Builder(domainSet, k, windowEpsilon, windowSize)
                    .setBucketParams(w, lambdaH)
                    .setHgRandom(hgRandom)
                    .build();
            case DIRECT:
                return new DirectHgHhLdpConfig
                    .Builder(domainSet, k, windowEpsilon, windowSize)
                    .setBucketParams(w, lambdaH)
                    .setHgRandom(hgRandom)
                    .build();
            case ADV:
                return new AdvHhgHhLdpConfig
                    .Builder(domainSet, k, windowEpsilon, windowSize)
                    .setBucketParams(w, lambdaH)
                    .setHgRandom(hgRandom)
                    .build();
            case BUFFER:
                return new BufferHhgHhLdpConfig
                    .Builder(domainSet, k, windowEpsilon, windowSize)
                    .setBucketParams(w, lambdaH)
                    .setHgRandom(hgRandom)
                    .build();
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
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
                return new FoHhLdpServer((FoHhLdpConfig) config);
            case BASIC:
                return new BasicHgHhLdpServer((BasicHgHhLdpConfig) config);
            case DIRECT:
                return new DirectHgHhLdpServer((DirectHgHhLdpConfig) config);
            case ADV:
                return new AdvHhgHhLdpServer((AdvHhgHhLdpConfig) config);
            case BUFFER:
                return new BufferHhgHhLdpServer((BufferHhgHhLdpConfig) config);
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
                return new FoHhLdpClient((FoHhLdpConfig) config);
            case BASIC:
                return new BasicHgHhLdpClient((BasicHgHhLdpConfig) config);
            case DIRECT:
                return new DirectHgHhLdpClient((DirectHgHhLdpConfig) config);
            case ADV:
                return new AdvHhgHhLdpClient((AdvHhgHhLdpConfig) config);
            case BUFFER:
                return new BufferHhgHhLdpClient((BufferHhgHhLdpConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
        }
    }
}
