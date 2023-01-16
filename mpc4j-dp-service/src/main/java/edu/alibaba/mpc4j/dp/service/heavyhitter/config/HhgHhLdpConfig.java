package edu.alibaba.mpc4j.dp.service.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory.HhLdpType;

import java.util.Set;

/**
 * Hot HeavyGuardian-based Heavy Hitter LDP config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class HhgHhLdpConfig extends HgHhLdpConfig {
    /**
     * the privacy allocation parameter α
     */
    private final double alpha;

    private HhgHhLdpConfig(Builder builder) {
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

    public static class Builder extends HgHhLdpConfig.Builder {
        /**
         * the privacy allocation parameter α
         */
        private double alpha;

        public Builder(HhLdpType type, Set<String> domainSet, int k, double windowEpsilon) {
            super(type, domainSet, k, windowEpsilon);
            setDefault();
        }

        public Builder(HhLdpConfig config) {
            super(config);
            if (config instanceof HhgHhLdpConfig) {
                HhgHhLdpConfig hhgHhLdpConfig = (HhgHhLdpConfig) config;
                alpha = hhgHhLdpConfig.alpha;
            } else {
                setDefault();
            }
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
                    throw new IllegalArgumentException("Invalid " + HhLdpType.class.getSimpleName() + ": " + type);
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
        public HhgHhLdpConfig build() {
            return new HhgHhLdpConfig(this);
        }
    }
}
