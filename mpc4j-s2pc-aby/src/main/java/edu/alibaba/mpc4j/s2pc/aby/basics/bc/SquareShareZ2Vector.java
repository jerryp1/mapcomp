package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.ShareVector;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * Square secret-share bit vector ([x]). The share is of the form: x = x_0 ⊕ x_1.
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public class SquareShareZ2Vector implements ShareZ2Vector {
    /**
     * the bit vector
     */
    private BitVector bitVector;
    /**
     * whether the share bit vector is in plain state.
     */
    private boolean plain;

    /**
     * Create a square-share bit vector.
     *
     * @param bitNum the number of bits.
     * @param bytes  the assigned bits represented by bytes.
     * @param plain  whether the bit vector is in plain state.
     * @return the created square-share bit vector.
     */
    public static SquareShareZ2Vector create(int bitNum, byte[] bytes, boolean plain) {
        SquareShareZ2Vector shareBitVector = new SquareShareZ2Vector();
        shareBitVector.bitVector = BitVectorFactory.create(bitNum, bytes);
        shareBitVector.plain = plain;

        return shareBitVector;
    }

    /**
     * Create a square-share bit vector.
     *
     * @param bitVector the bit vector.
     * @param plain     whether the bit vector is in plain state.
     * @return the created square-share bit vector.
     */
    public static SquareShareZ2Vector create(BitVector bitVector, boolean plain) {
        SquareShareZ2Vector shareBitVector = new SquareShareZ2Vector();
        shareBitVector.bitVector = bitVector;
        shareBitVector.plain = plain;

        return shareBitVector;
    }

    /**
     * Create a square-share bit vector, the given bit vector is copied.
     *
     * @param bitVector the bit vector.
     * @param plain     whether the bit vector is in plain state.
     * @return the created square-share bit vector.
     */
    public static SquareShareZ2Vector createCopy(BitVector bitVector, boolean plain) {
        SquareShareZ2Vector shareBitVector = new SquareShareZ2Vector();
        shareBitVector.bitVector = bitVector.copy();
        shareBitVector.plain = plain;

        return shareBitVector;
    }

    /**
     * Create a (secret) random square-share bit vector.
     *
     * @param bitNum       number of bits.
     * @param secureRandom the random states.
     * @return the created square-share bit vector.
     */
    public static SquareShareZ2Vector createRandom(int bitNum, SecureRandom secureRandom) {
        SquareShareZ2Vector shareBitVector = new SquareShareZ2Vector();
        shareBitVector.bitVector = BitVectorFactory.createRandom(bitNum, secureRandom);
        shareBitVector.plain = false;

        return shareBitVector;
    }

    /**
     * Create a (plain) square-share bit vector with all bits are 1.
     *
     * @param bitNum number of bits.
     * @return the created square-share bit vector.
     */
    public static SquareShareZ2Vector createOnes(int bitNum) {
        SquareShareZ2Vector squareShareBitVector = new SquareShareZ2Vector();
        squareShareBitVector.bitVector = BitVectorFactory.createOnes(bitNum);
        squareShareBitVector.plain = true;

        return squareShareBitVector;
    }

    /**
     * Create a (plain) square-share bit vector with all bits are 0.
     *
     * @param bitNum number of bits.
     * @return the created square-share bit vector.
     */
    public static SquareShareZ2Vector createZeros(int bitNum) {
        SquareShareZ2Vector squareShareBitVector = new SquareShareZ2Vector();
        squareShareBitVector.bitVector = BitVectorFactory.createZeros(bitNum);
        squareShareBitVector.plain = true;

        return squareShareBitVector;
    }

    /**
     * Create an empty square-share bit vector.
     *
     * @param plain whether the bit vector is in plain state.
     * @return the created square-share bit vector.
     */
    public static SquareShareZ2Vector createEmpty(boolean plain) {
        SquareShareZ2Vector squareShareBitVector = new SquareShareZ2Vector();
        squareShareBitVector.bitVector = BitVectorFactory.createEmpty();
        squareShareBitVector.plain = plain;

        return squareShareBitVector;
    }

    private SquareShareZ2Vector() {
        // empty
    }

    @Override
    public SquareShareZ2Vector copy() {
        SquareShareZ2Vector clone = new SquareShareZ2Vector();
        clone.bitVector = bitVector.copy();
        clone.plain = plain;

        return clone;
    }

    @Override
    public int getNum() {
        return bitVector.bitNum();
    }

    @Override
    public int getByteNum() {
        return bitVector.byteNum();
    }

    @Override
    public void replaceCopy(BitVector bitVector, boolean plain) {
        this.bitVector.replaceCopy(bitVector);
        this.plain = plain;
    }

    @Override
    public BitVector getBitVector() {
        return bitVector;
    }

    @Override
    public boolean get(int index) {
        return bitVector.get(index);
    }

    @Override
    public boolean isPlain() {
        return plain;
    }

    @Override
    public SquareShareZ2Vector split(int bitNum) {
        BitVector splitBitVector = bitVector.split(bitNum);
        return SquareShareZ2Vector.create(splitBitVector, plain);
    }

    @Override
    public void reduce(int bitNum) {
        bitVector.reduce(bitNum);
    }

    @Override
    public void merge(ShareVector other) {
        SquareShareZ2Vector that = (SquareShareZ2Vector) other;
        assert this.plain == that.isPlain() : "merged ones must have the same public state";
        bitVector.merge(that.getBitVector());
    }

    @Override
    public SquareShareZ2Vector xor(ShareZ2Vector that, boolean plain) {
        BitVector resultBitVector = bitVector.xor(that.getBitVector());
        return SquareShareZ2Vector.create(resultBitVector, plain);
    }

    @Override
    public void xori(ShareZ2Vector that, boolean plain) {
        bitVector.xori(that.getBitVector());
        this.plain = plain;
    }

    @Override
    public SquareShareZ2Vector and(ShareZ2Vector that) {
        BitVector resultBitVector = bitVector.and(that.getBitVector());
        return SquareShareZ2Vector.create(resultBitVector, plain && that.isPlain());
    }

    @Override
    public void andi(ShareZ2Vector that) {
        bitVector.andi(that.getBitVector());
        plain = plain && that.isPlain();
    }

    @Override
    public SquareShareZ2Vector or(ShareZ2Vector that) {
        BitVector resultBitVector = bitVector.or(that.getBitVector());
        return SquareShareZ2Vector.create(resultBitVector, plain && that.isPlain());
    }

    @Override
    public void ori(ShareZ2Vector that) {
        bitVector.ori(that.getBitVector());
        plain = plain && that.isPlain();
    }

    @Override
    public SquareShareZ2Vector not() {
        BitVector resultBitVector = bitVector.not();
        return SquareShareZ2Vector.create(resultBitVector, plain);
    }

    @Override
    public void noti() {
        bitVector.noti();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(bitVector)
            .append(plain)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SquareShareZ2Vector) {
            SquareShareZ2Vector that = (SquareShareZ2Vector) obj;
            return new EqualsBuilder()
                .append(this.bitVector, that.bitVector)
                .append(this.plain, that.plain)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", plain ? "plain" : "secret", bitVector.toString());
    }
}
