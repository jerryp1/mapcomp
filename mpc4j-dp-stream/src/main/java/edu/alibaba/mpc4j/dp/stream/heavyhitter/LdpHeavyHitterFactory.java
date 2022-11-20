package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import java.security.SecureRandom;
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
                return new BasicHgLdpHeavyHitter(domainSet, k, epsilon, new SecureRandom());
            case NAIVE:
                return new NaiveLdpHeavyHitter(domainSet, k, epsilon);
            case ADV_HEAVY_GUARDIAN:
                return new AdvHgLdpHeavyHitter(domainSet, k, epsilon, new SecureRandom());
            case RELAX_HEAVY_GUARDIAN:
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Heavy Hitter with Local Differential Privacy.
     *
     * @param type                the type of Heavy Hitter with Local Differential Privacy.
     * @param domainSet           the domain set.
     * @param k                   the heavy hitter num k.
     * @param epsilon             the privacy parameter ε.
     * @param heavyGuardianRandom the HeavyGuardian random state.
     * @return an instance of Heavy Hitter with Local Differential Privacy.
     */
    public static HgLdpHeavyHitter createHgInstance(LdpHeavyHitterType type, Set<String> domainSet,
                                                  int k, double epsilon, Random heavyGuardianRandom) {
        switch (type) {
            case BASIC_HEAVY_GUARDIAN:
                return new BasicHgLdpHeavyHitter(domainSet, k, epsilon, heavyGuardianRandom);
            case ADV_HEAVY_GUARDIAN:
                return new AdvHgLdpHeavyHitter(domainSet, k, epsilon, heavyGuardianRandom);
            case RELAX_HEAVY_GUARDIAN:
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }
}
