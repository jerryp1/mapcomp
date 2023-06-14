package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;

/**
 * Adder Factory.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class AdderFactory {
    /**
     * Private constructor.
     */
    private AdderFactory() {
        // empty
    }

    /**
     * Adder type enums.
     */
    public enum AdderTypes {
        /**
         * Ripple carry adder.
         */
        RIPPLE_CARRY,
        /**
         * Serial adder using linear prefix network, which essentially gives a Ripple Carry Adder in pre-computation mode.
         */
        SERIAL,
        /**
         * Parallel prefix adder using Sklansky structure. The structure comes from the following paper:
         *
         * <p>
         * Sklansky, Jack. "Conditional-sum addition logic." IRE Transactions on Electronic computers 2 (1960): 226-231.
         * </p>
         */
        SKLANSKY,
        /**
         * Parallel prefix adder using Brent-Kung structure. The structure comes from the following paper:
         *
         * <p>
         * Brent, and Kung. "A regular layout for parallel adders." IEEE transactions on Computers 100.3 (1982): 260-264.
         * </p>
         */
        BRENT_KUNG,
        /**
         * Parallel prefix adder using Kogge-Stone structure. The structure comes from the following paper:
         *
         * <p>
         * Kogge, Peter M., and Harold S. Stone. "A parallel algorithm for the efficient solution of a general class of
         * recurrence equations." IEEE transactions on computers 100.8 (1973): 786-793.
         * </p>
         */
        KOGGE_STONE,
    }

    /**
     * Creates a adder.
     *
     * @param type type of adder.
     * @param type z2 integer circuit.
     * @return a adder.
     */
    public static Adder createAdder(AdderTypes type, Z2IntegerCircuit circuit) {
        switch (type) {
            case RIPPLE_CARRY:
                return new RippleCarryAdder(circuit);
            case SERIAL:
                return new SerialAdder(circuit);
            case SKLANSKY:
                return new SklanskyAdder(circuit);
            case BRENT_KUNG:
                return new BrentKungAdder(circuit);
            case KOGGE_STONE:
                return new KoggeStoneAdder(circuit);
            default:
                throw new IllegalArgumentException("Invalid " + AdderFactory.AdderTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}
