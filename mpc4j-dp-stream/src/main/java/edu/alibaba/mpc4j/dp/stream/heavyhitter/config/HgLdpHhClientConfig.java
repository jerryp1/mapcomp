package edu.alibaba.mpc4j.dp.stream.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory.LdpHhType;

import java.util.Random;
import java.util.Set;

/**
 * HeavyGuardian-based LDP heavy hitter client config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class HgLdpHhClientConfig extends BasicLdpHhClientConfig {
    /**
     * budget num
     */
    private final int w;
    /**
     * λ_h, i.e., the cell num in each bucket
     */
    private final int lambdaH;

    protected HgLdpHhClientConfig(Builder builder) {
        super(builder);
        w = builder.w;
        lambdaH = builder.lambdaH;
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

    public static class Builder extends BasicLdpHhClientConfig.Builder {
        /**
         * budget num
         */
        private int w;
        /**
         * λ_h, i.e., the cell num in each bucket
         */
        private int lambdaH;

        public Builder(LdpHhType type, Set<String> domainSet, int k, double windowEpsilon) {
            super(type, domainSet, k, windowEpsilon);
            setDefault();
        }

        public Builder(LdpHhClientConfig clientConfig) {
            super(clientConfig.getType(), clientConfig.getDomainSet(), clientConfig.getK(), clientConfig.getWindowEpsilon());
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

        @Override
        public HgLdpHhClientConfig build() {
            return new HgLdpHhClientConfig(this);
        }
    }
}
