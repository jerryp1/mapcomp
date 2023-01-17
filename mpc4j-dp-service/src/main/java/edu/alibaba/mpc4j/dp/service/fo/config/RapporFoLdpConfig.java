package edu.alibaba.mpc4j.dp.service.fo.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.rappor.RapporFoLdpUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RAPPOR Frequency Oracle LDP config.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class RapporFoLdpConfig extends BasicFoLdpConfig {
    /**
     * number of cohorts.
     */
    private final int cohortNum;
    /**
     * number of hashes in each cohort.
     */
    private final int hashNum;
    /**
     * hash seeds
     */
    private final int[][] hashSeeds;
    /**
     * the IntHash type
     */
    private final IntHashFactory.IntHashType intHashType;
    /**
     * the size of the bloom filter
     */
    private final int m;

    protected RapporFoLdpConfig(Builder builder) {
        super(builder);
        cohortNum = builder.cohortNum;
        hashNum = builder.hashNum;
        m = RapporFoLdpUtils.getM(d, hashNum);
        int m = RapporFoLdpUtils.getM(d, hashNum);
        intHashType = builder.intHashType;
        IntHash intHash = IntHashFactory.createInstance(builder.intHashType);
        hashSeeds = IntStream.range(0, cohortNum)
            .mapToObj(cohortIndex -> {
                while (true) {
                    // we need to ensure that every hash seed group would have distinct hash values for all items.
                    int[] cohortHashSeeds = IntStream.range(0, hashNum).map(hashIndex -> builder.random.nextInt()).toArray();
                    Set<ByteBuffer> hashValueSet = domain.getDomainSet().stream()
                        .map(item -> {
                            int[] hashValues = RapporFoLdpUtils.hash(intHash, item, m, cohortHashSeeds);
                            Arrays.sort(hashValues);
                            return ByteBuffer.wrap(IntUtils.intArrayToByteArray(hashValues));
                        })
                        .collect(Collectors.toSet());
                    if (hashValueSet.size() == d) {
                        // all hash values are distinct
                        return cohortHashSeeds;
                    }
                }
            })
            .toArray(int[][]::new);
    }

    /**
     * Gets the number of cohorts.
     *
     * @return the number of cohorts.
     */
    public int getCohortNum() {
        return cohortNum;
    }

    /**
     * Gets the hash seeds.
     *
     * @return the hash seeds.
     */
    public int[][] getHashSeeds() {
        return hashSeeds;
    }

    /**
     * Gets the IntHash type.
     *
     * @return the IntHash type.
     */
    public IntHashFactory.IntHashType getIntHashType() {
        return intHashType;
    }

    /**
     * Gets the size of the bloom filter.
     *
     * @return the size of the bloom filter.
     */
    public int getM() {
        return m;
    }

    /**
     * Gets f, the probability used to perturb bloom filters.
     *
     * @return f.
     */
    public double getF() {
        // f = 2 / (e^{ε / 2k} + 1)
        return 2 / (Math.exp(epsilon / 2 / hashNum) + 1);
    }

    public static class Builder extends BasicFoLdpConfig.Builder {
        /**
         * number of cohorts.
         */
        private int cohortNum;
        /**
         * number of hashes in each cohort.
         */
        private int hashNum;
        /**
         * the randomness for generating the hash seeds
         */
        private Random random;
        /**
         * IntHash type
         */
        private IntHashFactory.IntHashType intHashType;

        public Builder(FoLdpFactory.FoLdpType type, Set<String> domainSet, double epsilon) {
            super(type, domainSet, epsilon);
            // default cohort num is 8, from pure-LDP
            cohortNum = 8;
            // default hash num is 2
            hashNum = 2;
            // default IntHash type
            intHashType = IntHashFactory.fastestType();
            // default random
            random = new Random();
        }

        /**
         * Sets the number of cohorts and the number of hashes in each cohort.
         *
         * @param cohortNum the number of cohorts.
         * @param hashNum the number of hashes in each cohort.
         * @return the builder.
         */
        public Builder setHashes(int cohortNum, int hashNum) {
            return setHashes(cohortNum, hashNum, new Random());
        }

        /**
         * Sets the number of cohorts and the number of hashes in each cohort.
         *
         * @param cohortNum the number of cohorts.
         * @param hashNum the number of hashes in each cohort.
         * @param random the random state used to generate the hash seeds.
         * @return the builder.
         */
        public Builder setHashes(int cohortNum, int hashNum, Random random) {
            MathPreconditions.checkPositive("# of cohorts", cohortNum);
            this.cohortNum = cohortNum;
            MathPreconditions.checkGreaterOrEqual("# of hashes", hashNum, 2);
            this.hashNum = hashNum;
            this.random = random;
            return this;
        }

        /**
         * Sets the IntHash type.
         *
         * @param intHashType the IntHash type.
         * @return the builder.
         */
        public Builder setIntHashType(IntHashFactory.IntHashType intHashType) {
            this.intHashType = intHashType;
            return this;
        }

        @Override
        public RapporFoLdpConfig build() {
            return new RapporFoLdpConfig(this);
        }
    }
}
