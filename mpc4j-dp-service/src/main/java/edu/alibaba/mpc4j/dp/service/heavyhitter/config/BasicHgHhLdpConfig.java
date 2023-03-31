package edu.alibaba.mpc4j.dp.service.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;

import java.util.Random;
import java.util.Set;

/**
 * Basic HeavyGuardian-based Heavy Hitter LDP config.
 *
 * @author Weiran Liu
 * @date 2023/3/21
 */
public class BasicHgHhLdpConfig extends BaseHhLdpConfig implements HgHhLdpConfig {
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

    protected BasicHgHhLdpConfig(Builder builder) {
        super(builder);
        w = builder.w;
        lambdaH = builder.lambdaH;
        hgRandom = builder.hgRandom;
    }

    @Override
    public int getW() {
        return w;
    }

    @Override
    public int getLambdaH() {
        return lambdaH;
    }

    @Override
    public Random getHgRandom() {
        return hgRandom;
    }

    public static class Builder extends BaseHhLdpConfig.Builder {
        /**
         * budget num
         */
        private int w;
        /**
         * λ_h, i.e., the cell num in each bucket
         */
        private int lambdaH;
        /**
         * HeavyGuardian random state, used only for the server
         */
        private Random hgRandom;

        public Builder(Set<String> domainSet, int k, double windowEpsilon, int windowSize) {
            super(HhLdpFactory.HhLdpType.BASIC, domainSet, k, windowEpsilon, windowSize);
            // set default values
            w = 1;
            lambdaH = k;
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

        @Override
        public BasicHgHhLdpConfig build() {
            return new BasicHgHhLdpConfig(this);
        }
    }
}
