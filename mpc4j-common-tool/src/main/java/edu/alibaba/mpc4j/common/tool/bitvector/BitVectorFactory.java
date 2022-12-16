package edu.alibaba.mpc4j.common.tool.bitvector;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * BitVector Factory.
 *
 * @author Weiran Liu
 * @date 2022/12/16
 */
public class BitVectorFactory {
    /**
     * BitVector type
     */
    public enum BitVectorType {
        /**
         * bit vector represented by bytes, use this if the bit vector is often used for operations.
         */
        BYTES_BIT_VECTOR,
        /**
         * bit vector represented by BigInteger, use this if the bit vector is often used for split / merge / reduce.
         */
        BIGINTEGER_BIT_VECTOR,
    }

    /**
     * Create with assigned bits.
     *
     * @param type   the BitVector type.
     * @param bitNum the number of bits.
     * @param bytes  the assigned bits represented by bytes.
     * @return the created bit vector.
     */
    public static BitVector create(BitVectorType type, int bitNum, byte[] bytes) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.create(bitNum, bytes);
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.create(bitNum, bytes);
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create with assigned bits.
     *
     * @param type       the BitVector type.
     * @param bitNum     the number of bits.
     * @param bigInteger the assigned bits represented by BigInteger.
     * @return the created bit vector.
     */
    public static BitVector create(BitVectorType type, int bitNum, BigInteger bigInteger) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.create(bitNum, bigInteger);
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.create(bitNum, bigInteger);
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create a random bit vector.
     *
     * @param type         the BitVector type.
     * @param bitNum       the number of bits.
     * @param secureRandom the random state.
     * @return the created bit vector.
     */
    public static BitVector createRandom(BitVectorType type, int bitNum, SecureRandom secureRandom) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.createRandom(bitNum, secureRandom);
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.createRandom(bitNum, secureRandom);
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create a bit vector with all bits are 1.
     *
     * @param type   the BitVector type.
     * @param bitNum the number of bits.
     * @return the created bit vector.
     */
    public static BitVector createOnes(BitVectorType type, int bitNum) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.createOnes(bitNum);
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.createOnes(bitNum);
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create a bit vector with all bits are 0.
     *
     * @param type   the BitVector type.
     * @param bitNum the number of bits.
     * @return the created bit vector.
     */
    public static BitVector createZeros(BitVectorType type, int bitNum) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.createZeros(bitNum);
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.createZeros(bitNum);
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }

    /**
     * Create an empty (0 number of bits) bit vector.
     *
     * @param type the BitVector type.
     * @return the created bit vector.
     */
    public static BitVector createEmpty(BitVectorType type) {
        switch (type) {
            case BYTES_BIT_VECTOR:
                return BytesBitVector.createEmpty();
            case BIGINTEGER_BIT_VECTOR:
                return BigIntegerBitVector.createEmpty();
            default:
                throw new IllegalArgumentException("Invalid " + BitVectorType.class.getSimpleName() + ": " + type);
        }
    }
}
