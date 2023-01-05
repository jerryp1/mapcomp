package edu.alibaba.mpc4j.dp.stream.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.stream.heavyhitter.LdpHhFactory.LdpHhType;

import java.util.Set;

/**
 * Basic LDP heavy hitter client config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class BasicLdpHhClientConfig implements LdpHhClientConfig {
    /**
     * the type
     */
    private final LdpHhType type;
    /**
     * the domain set
     */
    private final Set<String> domainSet;
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

    protected BasicLdpHhClientConfig(Builder builder) {
        type = builder.type;
        domainSet = builder.domainSet;
        d = builder.d;
        k = builder.k;
        windowEpsilon = builder.windowEpsilon;
    }

    @Override
    public LdpHhType getType() {
        return type;
    }

    @Override
    public Set<String> getDomainSet() {
        return domainSet;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BasicLdpHhClientConfig> {
        /**
         * the type
         */
        protected final LdpHhType type;
        /**
         * the domain set
         */
        private final Set<String> domainSet;
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

        public Builder(LdpHhType type, Set<String> domainSet, int k, double windowEpsilon) {
            this.type = type;
            d = domainSet.size();
            MathPreconditions.checkGreater("|Ω|", d, 1);
            this.domainSet = domainSet;
            MathPreconditions.checkPositiveInRangeClosed("k", k, d);
            this.k = k;
            MathPreconditions.checkPositive("ε / w", windowEpsilon);
            this.windowEpsilon = windowEpsilon;
        }

        @Override
        public BasicLdpHhClientConfig build() {
            return new BasicLdpHhClientConfig(this);
        }
    }
}
