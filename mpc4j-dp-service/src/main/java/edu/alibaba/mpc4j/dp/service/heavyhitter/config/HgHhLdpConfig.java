package edu.alibaba.mpc4j.dp.service.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory.HhLdpType;

import java.util.Random;
import java.util.Set;

/**
 * HeavyGuardian-based Heavy Hitter LDP config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class HgHhLdpConfig extends BasicHhLdpConfig {
    /**
     * budget num
     */
    private final int w;
    /**
     * λ_h, i.e., the cell num in each bucket
     */
    private final int lambdaH;
    /**
     * HeavyGuardian random state, used only for the server
     */
    private final Random hgRandom;
    /**
     * the privacy allocation parameter α
     */
    private final double alpha;
    /**
     * γ_h, set to negative if we do not manually set
     */
    private final double gammaH;

    protected HgHhLdpConfig(Builder builder) {
        super(builder);
        w = builder.w;
        lambdaH = builder.lambdaH;
        hgRandom = builder.hgRandom;
        alpha = builder.alpha;
        gammaH = builder.gammaH;
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
     * Gets the random state used in HeavyGuardian.
     *
     * @return the random state used in HeavyGuardian.
     */
    public Random getHgRandom() {
        return hgRandom;
    }

    /**
     * Gets the privacy allocation parameter α.
     *
     * @return the privacy allocation parameter α.
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * Gets γ_h. It can be negative if we do not manually set
     *
     * @return γ_h.
     */
    public double getGammaH() {
        return gammaH;
    }

    public static class Builder extends BasicHhLdpConfig.Builder {
        /**
         * budget num
         */
        protected int w;
        /**
         * λ_h, i.e., the cell num in each bucket
         */
        protected int lambdaH;
        /**
         * HeavyGuardian random state, used only for the server
         */
        protected Random hgRandom;
        /**
         * the privacy allocation parameter α
         */
        private double alpha;
        /**
         * γ_h, set to negative if we do not manually set
         */
        private double gammaH;

        public Builder(HhLdpType type, Set<String> domainSet, int k, double windowEpsilon) {
            super(type, domainSet, k, windowEpsilon);
            switch (type) {
                case BASIC:
                    break;
                case ADV:
                    alpha = 1.0 / 3;
                    break;
                case RELAX:
                    alpha = 1.0 / 2;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
            }
            // set default values
            w = 1;
            lambdaH = k;
            gammaH = -1;
            hgRandom = new Random();
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

        public Builder setHgRandom(Random hgRandom) {
            this.hgRandom = hgRandom;
            return this;
        }

        /**
         * Sets the privacy allocation parameter α.
         *
         * @param alpha the privacy allocation parameter α.
         */
        public Builder setAlpha(double alpha) {
            MathPreconditions.checkPositiveInRange("α", alpha, 1);
            this.alpha = alpha;
            return this;
        }

        public Builder setGammaH(double gammaH) {
            MathPreconditions.checkNonNegativeInRangeClosed("γ_h", gammaH, 1);
            this.gammaH = gammaH;
            return this;
        }

        @Override
        public HgHhLdpConfig build() {
            return new HgHhLdpConfig(this);
        }
    }
}
