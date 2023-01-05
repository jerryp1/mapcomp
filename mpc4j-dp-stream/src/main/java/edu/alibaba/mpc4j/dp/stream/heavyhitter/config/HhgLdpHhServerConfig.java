package edu.alibaba.mpc4j.dp.stream.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory.LdpHhType;

/**
 * HeavyGuardian-based LDP heavy hitter server config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class HhgLdpHhServerConfig extends HgLdpHhServerConfig {
    /**
     * the privacy allocation parameter α
     */
    private final double alpha;

    private HhgLdpHhServerConfig(Builder builder) {
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

    public static class Builder extends HgLdpHhServerConfig.Builder {
        /**
         * the privacy allocation parameter α
         */
        private double alpha;

        public Builder(LdpHhType type, int d, int k, double windowEpsilon) {
            super(type, d, k, windowEpsilon);
            setDefault();
        }

        public Builder(LdpHhServerConfig serverConfig) {
            super(serverConfig.getType(), serverConfig.getD(), serverConfig.getK(), serverConfig.getWindowEpsilon());
            setDefault();
        }

        public Builder(HgLdpHhServerConfig serverConfig) {
            super(serverConfig);
            setBucketParams(serverConfig.getW(), serverConfig.getLambdaH());
            setRandom(serverConfig.getRandom());
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
        public HhgLdpHhServerConfig build() {
            return new HhgLdpHhServerConfig(this);
        }
    }
}
