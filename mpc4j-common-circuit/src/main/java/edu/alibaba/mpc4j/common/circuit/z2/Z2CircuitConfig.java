package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.adder.AdderFactory;
import edu.alibaba.mpc4j.common.circuit.z2.multiplier.MultiplierFactory;

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
    /**
     * Multiplier type.
     */
    private MultiplierFactory.MultiplierTypes multiplierTypes;

    private Z2CircuitConfig(Builder builder) {
        setAdderType(builder.adderType);
        setMultiplierTypes(builder.multiplierTypes);
    }

    public AdderFactory.AdderTypes getAdderType() {
        return adderType;
    }

    public void setAdderType(AdderFactory.AdderTypes adderType) {
        this.adderType = adderType;
    }

    public MultiplierFactory.MultiplierTypes getMultiplierTypes() {
        return multiplierTypes;
    }

    public void setMultiplierTypes(MultiplierFactory.MultiplierTypes multiplierTypes) {
        this.multiplierTypes = multiplierTypes;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Z2CircuitConfig> {
        /**
         * Adder type.
         */
        private AdderFactory.AdderTypes adderType;
        /**
         * Multiplier type.
         */
        private MultiplierFactory.MultiplierTypes multiplierTypes;

        public Builder() {
            adderType = AdderFactory.AdderTypes.RIPPLE_CARRY;
            multiplierTypes = MultiplierFactory.MultiplierTypes.SHIFT_ADD;
        }

        public Builder setAdderType(AdderFactory.AdderTypes adderType) {
            this.adderType = adderType;
            return this;
        }

        public Builder setMultiplierTypes(MultiplierFactory.MultiplierTypes multiplierTypes) {
            this.multiplierTypes = multiplierTypes;
            return this;
        }

        @Override
        public Z2CircuitConfig build() {
            return new Z2CircuitConfig(this);
        }
    }
}
