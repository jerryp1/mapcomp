package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.adder.AdderFactory;

/**
 * Z2 Integer Circuit Config.
 *
 * @author Li Peng
 * @date 2023/6/2
 */
public class Z2CircuitConfig implements CircuitConfig {
    /**
     * Adder type.
     */
    private AdderFactory.AdderTypes adderType;

    private Z2CircuitConfig(Builder builder) {
        setAdderType(builder.adderType);
    }

    public AdderFactory.AdderTypes getAdderType() {
        return adderType;
    }

    public void setAdderType(AdderFactory.AdderTypes adderType) {
        this.adderType = adderType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Z2CircuitConfig> {
        /**
         * Adder type.
         */
        private AdderFactory.AdderTypes adderType;

        public Builder() {
            adderType = AdderFactory.AdderTypes.RIPPLE_CARRY;
        }

        public Builder setAdderType(AdderFactory.AdderTypes adderType) {
            this.adderType = adderType;
            return this;
        }

        @Override
        public Z2CircuitConfig build() {
            return new Z2CircuitConfig(this);
        }
    }
}
