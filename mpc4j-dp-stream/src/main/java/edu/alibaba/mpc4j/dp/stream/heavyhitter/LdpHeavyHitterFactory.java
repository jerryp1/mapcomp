package edu.alibaba.mpc4j.dp.stream.heavyhitter;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.hg.AdvHhgLdpHeavyHitter;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.hg.BasicHgLdpHeavyHitter;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.hg.RelaxHhgLdpHeavyHitter;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.naive.NaiveLdpHeavyHitter;

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
    public static LdpHeavyHitter createInstance(LdpHeavyHitterType type,
                                                Set<String> domainSet,
                                                int k, double windowEpsilon) {
        switch (type) {
            case BASIC_HG:
                return new BasicHgLdpHeavyHitter(domainSet, new Random(), k, windowEpsilon);
            case NAIVE_RR:
                return new NaiveLdpHeavyHitter(domainSet, k, windowEpsilon);
            case ADVAN_HG:
                return new AdvHhgLdpHeavyHitter(domainSet, new Random(), k, windowEpsilon);
            case RELAX_HG:
                return new RelaxHhgLdpHeavyHitter(domainSet, new Random(), k, windowEpsilon);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type          the type of Heavy Hitter with Local Differential Privacy.
     * @param domainSet     the domain set.
     * @param k             the heavy hitter num k.
     * @param windowEpsilon the privacy parameter ε / w.
     * @return an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HgLdpHeavyHitter createHgInstance(LdpHeavyHitterType type,
                                                    Set<String> domainSet,
                                                    int k, double windowEpsilon) {
        return createHgInstance(type, domainSet, new Random(), k, windowEpsilon);
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type                the type of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     * @param domainSet           the domain set.
     * @param heavyGuardianRandom the HeavyGuardian random state.
     * @param k                   the heavy hitter num k.
     * @param windowEpsilon       the privacy parameter ε / w.
     * @return an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HgLdpHeavyHitter createHgInstance(LdpHeavyHitterType type,
                                                    Set<String> domainSet, Random heavyGuardianRandom,
                                                    int k, double windowEpsilon) {
        switch (type) {
            case BASIC_HG:
                return new BasicHgLdpHeavyHitter(domainSet, heavyGuardianRandom, k, windowEpsilon);
            case ADVAN_HG:
                return new AdvHhgLdpHeavyHitter(domainSet, heavyGuardianRandom, k, windowEpsilon);
            case RELAX_HG:
                return new RelaxHhgLdpHeavyHitter(domainSet, heavyGuardianRandom, k, windowEpsilon);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type                the type of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     * @param domainSet           the domain set.
     * @param w                   the bucket num.
     * @param lambdaH             the cell num in each bucket.
     * @param heavyGuardianRandom the HeavyGuardian random state.
     * @param k                   the heavy hitter num k.
     * @param windowEpsilon       the privacy parameter ε / w.
     * @return an instance of HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HgLdpHeavyHitter createHgInstance(LdpHeavyHitterType type,
                                                    Set<String> domainSet, int w, int lambdaH, Random heavyGuardianRandom,
                                                    int k, double windowEpsilon) {
        switch (type) {
            case BASIC_HG:
                return new BasicHgLdpHeavyHitter(domainSet, w, lambdaH, 0, heavyGuardianRandom, k, windowEpsilon);
            case ADVAN_HG:
                return new AdvHhgLdpHeavyHitter(domainSet, w, lambdaH, 0, heavyGuardianRandom, k, windowEpsilon);
            case RELAX_HG:
                return new RelaxHhgLdpHeavyHitter(domainSet, w, lambdaH, 0, heavyGuardianRandom, k, windowEpsilon);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type          the type of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     * @param domainSet     the domain set.
     * @param k             the heavy hitter num k.
     * @param windowEpsilon the privacy parameter ε / w.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HhgLdpHeavyHitter createHhgInstance(LdpHeavyHitterType type,
                                                      Set<String> domainSet,
                                                      int k, double windowEpsilon) {
        return createHhgInstance(type, domainSet, new Random(), k, windowEpsilon);
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type                the type of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     * @param domainSet           the domain set.
     * @param heavyGuardianRandom the HeavyGuardian random state.
     * @param k                   the heavy hitter num k.
     * @param windowEpsilon       the privacy parameter ε / w.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HhgLdpHeavyHitter createHhgInstance(LdpHeavyHitterType type,
                                                      Set<String> domainSet, Random heavyGuardianRandom,
                                                      int k, double windowEpsilon) {
        switch (type) {
            case ADVAN_HG:
                return new AdvHhgLdpHeavyHitter(domainSet, heavyGuardianRandom, k, windowEpsilon);
            case RELAX_HG:
                return new RelaxHhgLdpHeavyHitter(domainSet, heavyGuardianRandom, k, windowEpsilon);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type          the type of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     * @param domainSet     the domain set.
     * @param k             the heavy hitter num k.
     * @param windowEpsilon the privacy parameter ε / w.
     * @param alpha         the privacy parameter percentage α.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HhgLdpHeavyHitter createHhgInstance(LdpHeavyHitterType type,
                                                      Set<String> domainSet, int k,
                                                      double windowEpsilon, double alpha) {
        return createHhgInstance(type, domainSet, new Random(), k, windowEpsilon, alpha);
    }

    /**
     * Create an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     *
     * @param type                the type of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     * @param domainSet           the domain set.
     * @param k                   the heavy hitter num k.
     * @param windowEpsilon       the privacy parameter ε / w.
     * @param alpha               the privacy parameter percentage α.
     * @param heavyGuardianRandom the HeavyGuardian random state.
     * @return an instance of Hot HeavyGuardian-based Heavy Hitter with Local Differential Privacy.
     */
    public static HhgLdpHeavyHitter createHhgInstance(LdpHeavyHitterType type,
                                                      Set<String> domainSet, Random heavyGuardianRandom,
                                                      int k, double windowEpsilon, double alpha) {
        switch (type) {
            case ADVAN_HG:
                return new AdvHhgLdpHeavyHitter(domainSet, heavyGuardianRandom, k, windowEpsilon, alpha);
            case RELAX_HG:
                return new RelaxHhgLdpHeavyHitter(domainSet, heavyGuardianRandom, k, windowEpsilon, alpha);
            default:
                throw new IllegalArgumentException("Invalid " + LdpHeavyHitterType.class.getSimpleName() + ": " + type);
        }
    }
}
