package edu.alibaba.mpc4j.dp.stream.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory.LdpHhType;

/**
 * Basic LDP heavy hitter server config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class BasicLdpHhServerConfig implements LdpHhServerConfig {
    /**
     * the type
     */
    private final LdpHhType type;
    /**
     * the domain size d
     */
    private final int d;
    /**
     * the number of Heavy Hitters k
     */
    private final int k;
    /**
     * the privacy parameter ε / w
     */
    private final double windowEpsilon;

    protected BasicLdpHhServerConfig(Builder builder) {
        type = builder.type;
        d = builder.d;
        k = builder.k;
        windowEpsilon = builder.windowEpsilon;
    }

    @Override
    public LdpHhType getType() {
        return type;
    }

    @Override
    public int getD() {
        return d;
    }

    @Override
    public int getK() {
        return k;
    }

    @Override
    public double getWindowEpsilon() {
        return windowEpsilon;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BasicLdpHhServerConfig> {
        /**
         * the type
         */
        protected final LdpHhType type;
        /**
         * the domain size d
         */
        private final int d;
        /**
         * the number of Heavy Hitters k
         */
        protected final int k;
        /**
         * the privacy parameter ε / w
         */
        private final double windowEpsilon;

        public Builder(LdpHhType type, int d, int k, double windowEpsilon) {
            this.type = type;
            MathPreconditions.checkGreater("|Ω|", d, 1);
            this.d = d;
            MathPreconditions.checkPositiveInRangeClosed("k", k, d);
            this.k = k;
            MathPreconditions.checkPositive("ε / w", windowEpsilon);
            this.windowEpsilon = windowEpsilon;
        }

        @Override
        public BasicLdpHhServerConfig build() {
            return new BasicLdpHhServerConfig(this);
        }
    }
}
