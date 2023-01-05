package edu.alibaba.mpc4j.dp.stream.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory.LdpHhType;

import java.util.Set;

/**
 * HeavyGuardian-based LDP heavy hitter client config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class HhgLdpHhClientConfig extends HgLdpHhClientConfig {
    /**
     * the privacy allocation parameter α
     */
    private final double alpha;

    private HhgLdpHhClientConfig(Builder builder) {
        super(builder);
        alpha = builder.alpha;
    }

    /**
     * Gets the privacy allocation parameter α.
     *
     * @return the privacy allocation parameter α.
     */
    public double getAlpha() {
        return alpha;
    }

    public static class Builder extends HgLdpHhClientConfig.Builder {
        /**
         * the privacy allocation parameter α
         */
        private double alpha;

        public Builder(LdpHhType type, Set<String> domainSet, int k, double windowEpsilon) {
            super(type, domainSet, k, windowEpsilon);
            setDefault();
        }

        public Builder(LdpHhClientConfig clientConfig) {
            super(clientConfig.getType(), clientConfig.getDomainSet(), clientConfig.getK(), clientConfig.getWindowEpsilon());
            setDefault();
        }

        public Builder(HgLdpHhClientConfig clientConfig) {
            super(clientConfig.getType(), clientConfig.getDomainSet(), clientConfig.getK(), clientConfig.getWindowEpsilon());
            setBucketParams(clientConfig.getW(), clientConfig.getLambdaH());
            setDefault();
        }

        private void setDefault() {
            switch (type) {
                case ADVAN_HG:
                    alpha = 1.0 / 3;
                    break;
                case RELAX_HG:
                    alpha = 1.0 / 2;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid " + LdpHhType.class.getSimpleName() + ": " + type);
            }
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

        @Override
        public HhgLdpHhClientConfig build() {
            return new HhgLdpHhClientConfig(this);
        }
    }
}
