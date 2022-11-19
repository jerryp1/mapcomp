package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import java.util.Random;
import java.util.Set;

/**
 * Heavy Hitter with Local Differential Privacy Factory.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class LdpHeavyHitterFactory {

    private LdpHeavyHitterFactory() {
        // empty
    }

    public enum LdpHeavyHitterType {
        /**
         * naive
         */
        NAIVE,
        /**
         * basic HeavyGuardian
         */
        BASIC_HEAVY_GUARDIAN,
        /**
         * Advanced HeavyGuardian
         */
        ADV_HEAVY_GUARDIAN,
        /**
         * Related HeavyGuardian
         */
        RELAX_HEAVY_GUARDIAN,
    }

    /**
     * Create an instance of Heavy Hitter with Local Differential Privacy.
     *
     * @param type      the type of Heavy Hitter with Local Differential Privacy.
     * @param domainSet the domain set.
     * @param k         the heavy hitter num k.
     * @param epsilon   the privacy parameter ε.
     * @return an instance of Heavy Hitter with Local Differential Privacy.
     */
    public static LdpHeavyHitter createInstance(LdpHeavyHitterType type, Set<String> domainSet, int k, double epsilon) {
        switch (type) {
            case BASIC_HEAVY_GUARDIAN:
                return new BasicHgLdpHeavyHitter(domainSet, k, epsilon);
            case NAIVE:
                return new NaiveLdpHeavyHitter(domainSet, k, epsilon);
            case ADV_HEAVY_GUARDIAN:
            case RELAX_HEAVY_GUARDIAN:
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type      the type of Heavy Hitter with Local Differential Privacy.
     * @param domainSet the domain set.
     * @param k         the heavy hitter num k.
     * @param epsilon   the privacy parameter ε.
     * @param warmupNum the number of items the server used for warming up.
     * @param random    the random state.
     * @return an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HgLdpHeavyHitter createHgInstance(LdpHeavyHitterType type, Set<String> domainSet, int k, double epsilon,
                                                  int warmupNum, Random random) {
        switch (type) {
            case BASIC_HEAVY_GUARDIAN:
                return new BasicHgLdpHeavyHitter(domainSet, k, epsilon, random);
            case ADV_HEAVY_GUARDIAN:
            case RELAX_HEAVY_GUARDIAN:
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }
}
