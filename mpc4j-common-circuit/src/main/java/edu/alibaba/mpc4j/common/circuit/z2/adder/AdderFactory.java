package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;

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
         * Serial adder using linear prefix network, which essentially gives Ripple Carry Adder in pre-computation mode.
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
        BRENT_KUNG;
    }

    /**
     * Creates a adder.
     *
     * @param party party.
     * @param type  type of adder.
     * @return a adder.
     */
    public static Adder createAdder(MpcZ2cParty party, AdderTypes type) {
        switch (type) {
            case RIPPLE_CARRY:
                return new RippleCarryAdder(party);
            case SERIAL:
                return new SerialAdder(party);
            case SKLANSKY:
                return new SklanskyAdder(party);
            case BRENT_KUNG:
                return new BrentKungAdder(party);
            default:
                throw new IllegalArgumentException("Invalid " + AdderFactory.AdderTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}
