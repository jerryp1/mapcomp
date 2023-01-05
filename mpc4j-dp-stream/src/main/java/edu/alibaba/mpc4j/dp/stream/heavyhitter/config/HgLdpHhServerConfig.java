package edu.alibaba.mpc4j.dp.stream.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory.LdpHhType;

import java.util.Random;

/**
 * HeavyGuardian-based LDP heavy hitter server config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class HgLdpHhServerConfig extends BasicLdpHhServerConfig {
    /**
     * budget num
     */
    private final int w;
    /**
     * λ_h, i.e., the cell num in each bucket
     */
    private final int lambdaH;
    /**
     * random state for HeavyGuardian
     */
    private final Random random;

    protected HgLdpHhServerConfig(Builder builder) {
        super(builder);
        w = builder.w;
        lambdaH = builder.lambdaH;
        random = builder.random;
    }

    /**
     * Gets the bucket num.
     *
     * @return the bucket num.
     */
    public int getW() {
        return w;
    }

    /**
     * Gets λ_h, i.e., the cell num in each bucket.
     *
     * @return λ_h.
     */
    public int getLambdaH() {
        return lambdaH;
    }

    /**
     * Gets the random state.
     *
     * @return the random state.
     */
    public Random getRandom() {
        return random;
    }

    public static class Builder extends BasicLdpHhServerConfig.Builder {
        /**
         * budget num
         */
        private int w;
        /**
         * λ_h, i.e., the cell num in each bucket
         */
        private int lambdaH;
        /**
         * random state for HeavyGuardian
         */
        private Random random;

        public Builder(LdpHhType type, int d, int k, double windowEpsilon) {
            super(type, d, k, windowEpsilon);
            setDefault();
        }

        public Builder(LdpHhServerConfig serverConfig) {
            super(serverConfig.getType(), serverConfig.getD(), serverConfig.getK(), serverConfig.getWindowEpsilon());
            setDefault();
        }

        private void setDefault() {
            switch (type) {
                case BASIC_HG:
                case ADVAN_HG:
                case RELAX_HG:
                    break;
                default:
                    throw new IllegalArgumentException("Invalid " + LdpHhType.class.getSimpleName() + ": " + type);
            }
            // set default values
            w = 1;
            lambdaH = k;
            random = new Random();
        }

        /**
         * Sets the bucket parameters.
         *
         * @param w the bucket num.
         * @param lambdaH λ_h, i.e., the cell num in each bucket.
         */
        public Builder setBucketParams(int w, int lambdaH) {
            MathPreconditions.checkPositive("w (# of buckets)", w);
            MathPreconditions.checkPositive("λ_h (# of heavy part)", lambdaH);
            MathPreconditions.checkGreaterOrEqual("λ_h * w", lambdaH * w, k);
            this.w = w;
            this.lambdaH = lambdaH;
            return this;
        }

        /**
         * Sets the random state.
         *
         * @param random the random state.
         */
        public Builder setRandom(Random random) {
            this.random = random;
            return this;
        }

        @Override
        public HgLdpHhServerConfig build() {
            return new HgLdpHhServerConfig(this);
        }
    }
}
