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
         * Manchester carry-chain adder.
         */
        MANCHESTER,
        /**
         * Parallel prefix adder using Sklansky structure. TODO 待添加论文链接
         */
        SKLANSKY,
        /**
         * Parallel prefix adder using Brent-Kung structure. TODO 待添加论文链接
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
            case MANCHESTER:
                return new ManchesterAdder(party);
            case SKLANSKY:
                return new SklanskyAdder(party);
            case BRENT_KUNG:
                return new BrentKungAdder(party);
            default:
                throw new IllegalArgumentException("Invalid " + AdderFactory.AdderTypes.class.getSimpleName() + ": " + type.name());
        }
    }
}
