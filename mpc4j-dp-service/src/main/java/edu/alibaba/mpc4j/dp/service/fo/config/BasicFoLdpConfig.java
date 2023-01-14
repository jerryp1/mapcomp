package edu.alibaba.mpc4j.dp.service.fo.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;

import java.util.Set;

/**
 * Basic Frequency Oracle LDP config.
 *
 * @author Weiran Liu
 * @date 2023/1/14
 */
public class BasicFoLdpConfig implements FoLdpConfig {
    /**
     * the type
     */
    private final FoLdpFactory.FoLdpType type;
    /**
     * the domain set
     */
    private final Set<String> domainSet;
    /**
     * the domain size d
     */
    private final int d;
    /**
     * the privacy parameter ε
     */
    private final double epsilon;

    protected BasicFoLdpConfig(Builder builder) {
        type = builder.type;
        domainSet = builder.domainSet;
        d = builder.d;
        epsilon = builder.epsilon;
    }

    @Override
    public FoLdpFactory.FoLdpType getType() {
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
    public double getEpsilon() {
        return epsilon;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BasicFoLdpConfig> {
        /**
         * the type
         */
        protected final FoLdpFactory.FoLdpType type;
        /**
         * the domain set
         */
        private final Set<String> domainSet;
        /**
         * the domain size d
         */
        private final int d;
        /**
         * the privacy parameter ε
         */
        private final double epsilon;

        public Builder(FoLdpFactory.FoLdpType type, Set<String> domainSet, double epsilon) {
            this.type = type;
            d = domainSet.size();
            MathPreconditions.checkGreater("|Ω|", d, 1);
            this.domainSet = domainSet;
            MathPreconditions.checkPositive("ε", epsilon);
            this.epsilon = epsilon;
        }

        @Override
        public BasicFoLdpConfig build() {
            return new BasicFoLdpConfig(this);
        }
    }
}
