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
        NAIVE_RR,
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
     * Create an instance of Heavy Hitter with Local Differential Privacy.
     *
     * @param type          the type of Heavy Hitter with Local Differential Privacy.
     * @param domainSet     the domain set.
     * @param k             the heavy hitter num k.
     * @param windowEpsilon the privacy parameter ε / w.
     * @return an instance of Heavy Hitter with Local Differential Privacy.
     */
    public static LdpHeavyHitter createInstance(LdpHeavyHitterType type, Set<String> domainSet, int k,
                                                double windowEpsilon) {
        switch (type) {
            case BASIC_HG:
                return new BasicHgLdpHeavyHitter(domainSet, k, windowEpsilon, new Random());
            case NAIVE_RR:
                return new NaiveLdpHeavyHitter(domainSet, k, windowEpsilon);
            case ADVAN_HG:
                return new AdvHhgLdpHeavyHitter(domainSet, k, windowEpsilon, new Random());
            case RELAX_HG:
                return new RelaxHhgLdpHeavyHitter(domainSet, k, windowEpsilon, new Random());
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type                the type of Heavy Hitter with Local Differential Privacy.
     * @param domainSet           the domain set.
     * @param k                   the heavy hitter num k.
     * @param windowEpsilon       the privacy parameter ε / w.
     * @return an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HgLdpHeavyHitter createHgInstance(LdpHeavyHitterType type, Set<String> domainSet, int k,
                                                    double windowEpsilon) {
        return createHgInstance(type, domainSet, k, windowEpsilon, new Random());
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type                the type of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     * @param domainSet           the domain set.
     * @param k                   the heavy hitter num k.
     * @param windowEpsilon       the privacy parameter ε / w.
     * @param heavyGuardianRandom the HeavyGuardian random state.
     * @return an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HgLdpHeavyHitter createHgInstance(LdpHeavyHitterType type, Set<String> domainSet, int k,
                                                    double windowEpsilon, Random heavyGuardianRandom) {
        switch (type) {
            case BASIC_HG:
                return new BasicHgLdpHeavyHitter(domainSet, k, windowEpsilon, heavyGuardianRandom);
            case ADVAN_HG:
                return new AdvHhgLdpHeavyHitter(domainSet, k, windowEpsilon, heavyGuardianRandom);
            case RELAX_HG:
                return new RelaxHhgLdpHeavyHitter(domainSet, k, windowEpsilon, heavyGuardianRandom);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type                the type of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     * @param domainSet           the domain set.
     * @param k                   the heavy hitter num k.
     * @param windowEpsilon       the privacy parameter ε / w.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HhgLdpHeavyHitter createHhgInstance(LdpHeavyHitterType type, Set<String> domainSet, int k,
                                                      double windowEpsilon, double alpha) {
        return createHhgInstance(type, domainSet, k, windowEpsilon, alpha, new Random());
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type                the type of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     * @param domainSet           the domain set.
     * @param k                   the heavy hitter num k.
     * @param windowEpsilon       the privacy parameter ε / w.
     * @param heavyGuardianRandom the HeavyGuardian random state.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HhgLdpHeavyHitter createHhgInstance(LdpHeavyHitterType type, Set<String> domainSet, int k,
                                                      double windowEpsilon, double alpha, Random heavyGuardianRandom) {
        switch (type) {
            case ADVAN_HG:
                return new AdvHhgLdpHeavyHitter(domainSet, k, windowEpsilon, alpha, heavyGuardianRandom);
            case RELAX_HG:
                return new RelaxHhgLdpHeavyHitter(domainSet, k, windowEpsilon, alpha, heavyGuardianRandom);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }
}
