package edu.alibaba.mpc4j.dp.service.fo.config;

import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility-optimized randomized response config.
 *
 * @author Li Peng
 * @date 2023/8/28
 */
public class RrUldpConfig extends BasicFoLdpConfig {
    /**
     * the domain set of sensitive values
     */
    private final Set<String> sensDomainSet;

    private RrUldpConfig(RrUldpConfig.Builder builder) {
        super(builder);
        this.sensDomainSet = builder.sensDomainSet;
    }

    public Set<String> getSensDomainSet() {
        return sensDomainSet;
    }

    public static class Builder extends BasicFoLdpConfig.Builder {
        /**
         * the domain set of sensitive values
         */
        private Set<String> sensDomainSet;

        public Builder(FoLdpFactory.FoLdpType type, Set<String> domainSet, double epsilon) {
            super(type, domainSet, epsilon);
            String[] domains = domainSet.toArray(new String[0]);
            sensDomainSet = Arrays.stream(domains, 0, domains.length / 2).collect(Collectors.toSet());
        }

        public void setSensDomainSet(Set<String> sensDomainSet) {
            this.sensDomainSet = sensDomainSet;
        }

        @Override
        public RrUldpConfig build() {
            assert sensDomainSet != null : "The domain set of sensitive values must be set.";
            return new RrUldpConfig(this);
        }
    }
}
