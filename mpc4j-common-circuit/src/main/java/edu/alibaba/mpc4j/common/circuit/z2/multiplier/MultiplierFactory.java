package edu.alibaba.mpc4j.common.circuit.z2.multiplier;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.circuit.z2.adder.Adder;

/**
 * Multiplier Factory.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class MultiplierFactory {
    /**
     * Private constructor.
     */
    private MultiplierFactory() {
        // empty
    }

    /**
     * Multiplier type enums.
     */
    public enum MultiplierTypes {
        /**
         * Shift/add multiplier.
         */
        SHIFT_ADD,
    }

    /**
     * Creates a multiplier.
     *
     * @param party party.
     * @param type  type of adder.
     * @return a adder.
     */
    public static Multiplier createMultiplier(MpcZ2cParty party, MultiplierTypes type, Adder adder) {
        switch (type) {
            case SHIFT_ADD:
                return new ShiftAddMultiplier(party, adder);
            default:
                throw new IllegalArgumentException("Invalid " + MultiplierTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}
