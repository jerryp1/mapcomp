package com.alibaba.mpc4j.common.circuit.z2;

import java.util.Random;

/**
 * Mpc Z2 Vector Factory.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/21
 */
public class MpcZ2VectorFactory {

    /**
     * Create a random mpc bit vector.
     *
     * @param type   the MpcZ2Vector type.
     * @param bitNum the number of bits.
     * @param random the random state.
     * @return the created mpc z2 vector.
     */
    public static MpcZ2Vector createRandom(MpcZ2Type type, int bitNum, Random random) {
        switch (type) {
            case PLAIN:
                return PlainZ2Vector.createRandom(bitNum, random);
            default:
                throw new IllegalArgumentException("Invalid " + MpcZ2Type.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create a mpc z2 vector with all bits are 1.
     *
     * @param type   the MpcZ2Vector type.
     * @param bitNum the number of bits.
     * @return the created mpc z2 vector.
     */
    public static MpcZ2Vector createOnes(MpcZ2Type type, int bitNum) {
        switch (type) {
            case PLAIN:
                return PlainZ2Vector.createOnes(bitNum);
            default:
                throw new IllegalArgumentException("Invalid " + MpcZ2Type.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create a bit vector with all bits are 0.
     *
     * @param type   the MpcZ2Vector type.
     * @param bitNum the number of bits.
     * @return the created mpc z2 vector.
     */
    public static MpcZ2Vector createZeros(MpcZ2Type type, int bitNum) {
        switch (type) {
            case PLAIN:
                return PlainZ2Vector.createZeros(bitNum);
            default:
                throw new IllegalArgumentException("Invalid " + MpcZ2Vector.class.getSimpleName() + ": " + type);
        }
    }


    /**
     * Create an empty (0 number of bits) mpc z2 vector.
     *
     * @param type the MpcBitVector type.
     * @return the created mpc z2 vector.
     */
    public static MpcZ2Vector createEmpty(MpcZ2Type type) {
        switch (type) {
            case PLAIN:
                return PlainZ2Vector.createEmpty();
            default:
                throw new IllegalArgumentException("Invalid " + MpcZ2Type.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an mpc z2 vector with specific (plain) boolean value.
     *
     * @param type   the MpcBitVector type.
     * @param bitNum the number of bits.
     * @param value  the specific (plain) boolean value.
     * @return the created mpc z2 vector.
     */
    public static MpcZ2Vector create(MpcZ2Type type, int bitNum, boolean value) {
        switch (type) {
            case PLAIN:
                return PlainZ2Vector.create(bitNum, value);
            default:
                throw new IllegalArgumentException("Invalid " + MpcZ2Type.class.getSimpleName() + ": " + type);
        }
    }
}
