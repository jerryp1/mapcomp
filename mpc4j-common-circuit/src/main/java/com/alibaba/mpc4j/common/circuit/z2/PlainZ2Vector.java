package com.alibaba.mpc4j.common.circuit.z2;

import com.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;

import java.util.Random;

/**
 * Plain Z2 Vector.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/20
 */
public class PlainZ2Vector implements MpcZ2Vector {
    /**
     * the bit vector
     */
    private BitVector bitVector;

    @Override
    public int getByteNum() {
        return bitVector.byteNum();
    }

    @Override
    public boolean get(int index) {
        return bitVector.get(index);
    }

    @Override
    public MpcZ2Vector xor(MpcZ2Vector that, boolean plain) {
        assert plain : "PlainZ2Vector must be plain";
        return PlainZ2Vector.create(bitVector.xor(((PlainZ2Vector) that).getBitVector()));
    }

    @Override
    public void xori(MpcZ2Vector that, boolean plain) {
        assert plain : "PlainZ2Vector must be plain";
        this.bitVector = this.bitVector.xor(((PlainZ2Vector) that).getBitVector());
    }

    @Override
    public MpcZ2Vector and(MpcZ2Vector that) {
        return PlainZ2Vector.create(bitVector.and(((PlainZ2Vector) that).getBitVector()));
    }

    @Override
    public void andi(MpcZ2Vector that) {
        this.bitVector = this.bitVector.and(((PlainZ2Vector) that).getBitVector());
    }

    @Override
    public MpcZ2Vector or(MpcZ2Vector that) {
        return PlainZ2Vector.create(bitVector.or(((PlainZ2Vector) that).getBitVector()));
    }

    @Override
    public void ori(MpcZ2Vector that) {
        this.bitVector = this.bitVector.or(((PlainZ2Vector) that).getBitVector());
    }

    @Override
    public MpcZ2Vector not() {
        return PlainZ2Vector.create(bitVector.not());
    }

    @Override
    public void noti() {
        this.bitVector.noti();
    }

    @Override
    public MpcZ2Type getType() {
        return MpcZ2Type.PLAIN;
    }

    /**
     * Create a random plain z2 vector.
     *
     * @param bitNum the bit num.
     * @param random the random states.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createRandom(int bitNum, Random random) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createRandom(bitNum, random);
        return plainZ2Vector;
    }

    /**
     * Create a plain all-one z2 vector.
     *
     * @param bitNum the bit num.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createOnes(int bitNum) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createOnes(bitNum);
        return plainZ2Vector;
    }

    /**
     * Create a plain all-zero z2 vector.
     *
     * @param bitNum the bit num.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createZeros(int bitNum) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createZeros(bitNum);
        return plainZ2Vector;
    }

    /**
     * Create an empty plain z2 vector.
     *
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector createEmpty() {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = BitVectorFactory.createEmpty();
        return plainZ2Vector;
    }

    /**
     * Create a plain z2 vector with all bits equal assigned boolean.
     *
     * @param bitNum the bit num.
     * @param value  assigned value.
     * @return a plain z2 vector.
     */
    public static PlainZ2Vector create(int bitNum, boolean value) {
        return value ? PlainZ2Vector.createOnes(bitNum) : PlainZ2Vector.createZeros(bitNum);
    }

    /**
     * Get the inner bit vector.
     *
     * @return the inner bit vector.
     */
    public BitVector getBitVector() {
        return bitVector;
    }

    public static PlainZ2Vector create(BitVector bitVector) {
        PlainZ2Vector plainZ2Vector = new PlainZ2Vector();
        plainZ2Vector.bitVector = bitVector;
        return plainZ2Vector;
    }

    @Override
    public boolean isPlain() {
        return true;
    }

    @Override
    public MpcVector copy() {
        PlainZ2Vector clone = new PlainZ2Vector();
        clone.bitVector = bitVector.copy();

        return clone;
    }

    @Override
    public int getNum() {
        return bitVector.bitNum();
    }

    @Override
    public MpcVector split(int splitNum) {
        BitVector splitBitVector = bitVector.split(splitNum);
        return PlainZ2Vector.create(splitBitVector);
    }

    @Override
    public void reduce(int reduceNum) {
        bitVector.reduce(reduceNum);
    }

    @Override
    public void merge(MpcVector other) {
        PlainZ2Vector that = (PlainZ2Vector) other;
        bitVector.merge(that.getBitVector());
    }
}
