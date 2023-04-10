package edu.alibaba.mpc4j.s2pc.aby.basics.ac.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.crypto.matrix.vector.RingVector;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.ShareVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.ac.ShareRingVector;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Square share Zl vector ([x]). The share is of the form: x = x_0 + x_1.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class SquareShareZlVector implements ShareRingVector {
    /**
     * the vector
     */
    private ZlVector vector;
    /**
     * the plain state.
     */
    private boolean plain;

    /**
     * Create a share vector.
     *
     * @param zl       Zl instance.
     * @param elements the elements.
     * @param plain    the plain state.
     * @return a share vector.
     */
    public static SquareShareZlVector create(Zl zl, BigInteger[] elements, boolean plain) {
        SquareShareZlVector shareVector = new SquareShareZlVector();
        shareVector.vector = ZlVector.create(zl, elements);
        shareVector.plain = plain;

        return shareVector;
    }

    /**
     * Create a share vector.
     *
     * @param vector the vector.
     * @param plain  the plain state.
     * @return a share vector.
     */
    public static SquareShareZlVector create(ZlVector vector, boolean plain) {
        SquareShareZlVector shareVector = new SquareShareZlVector();
        shareVector.vector = vector;
        shareVector.plain = plain;

        return shareVector;
    }

    /**
     * Create a share vector, the given vector is copied.
     *
     * @param vector the vector.
     * @param plain  the plain state.
     * @return a share vector.
     */
    public static SquareShareZlVector createCopy(ZlVector vector, boolean plain) {
        SquareShareZlVector shareVector = new SquareShareZlVector();
        shareVector.vector = vector.copy();
        shareVector.plain = plain;

        return shareVector;
    }

    /**
     * Create a (plain) random share vector.
     *
     * @param zl           Zl instance.
     * @param num          the num.
     * @param secureRandom the random states.
     * @return a share vector.
     */
    public static SquareShareZlVector createRandom(Zl zl, int num, SecureRandom secureRandom) {
        SquareShareZlVector shareVector = new SquareShareZlVector();
        shareVector.vector = ZlVector.createRandom(zl, num, secureRandom);
        shareVector.plain = true;

        return shareVector;
    }

    /**
     * Create a (plain) all-one share vector.
     *
     * @param zl  Zl instance.
     * @param num the num.
     * @return a share vector.
     */
    public static SquareShareZlVector createOnes(Zl zl, int num) {
        SquareShareZlVector shareVector = new SquareShareZlVector();
        shareVector.vector = ZlVector.createOnes(zl, num);
        shareVector.plain = true;

        return shareVector;
    }

    /**
     * Create a (plain) all-zero share vector.
     *
     * @param zl  Zl instance.
     * @param num the num.
     * @return a share vector.
     */
    public static SquareShareZlVector createZeros(Zl zl, int num) {
        SquareShareZlVector shareVector = new SquareShareZlVector();
        shareVector.vector = ZlVector.createZeros(zl, num);
        shareVector.plain = true;

        return shareVector;
    }

    /**
     * Create an empty share vector.
     *
     * @param zl  Zl instance.
     * @param plain the plain state.
     * @return a share vector.
     */
    public static SquareShareZlVector createEmpty(Zl zl, boolean plain) {
        SquareShareZlVector shareVector = new SquareShareZlVector();
        shareVector.vector = ZlVector.createEmpty(zl);
        shareVector.plain = plain;

        return shareVector;
    }

    private SquareShareZlVector() {
        // empty
    }

    @Override
    public SquareShareZlVector copy() {
        SquareShareZlVector clone = new SquareShareZlVector();
        clone.vector = vector.copy();
        clone.plain = plain;

        return clone;
    }

    @Override
    public int getNum() {
        return vector.getNum();
    }

    @Override
    public void replaceCopy(RingVector vector, boolean plain) {
        this.vector.replaceCopy(vector);
        this.plain = plain;
    }

    @Override
    public SquareShareZlVector add(ShareRingVector other, boolean plain) {
        SquareShareZlVector that = (SquareShareZlVector) other;
        ZlVector resultVector = vector.add(that.vector);
        return SquareShareZlVector.create(resultVector, plain);
    }

    @Override
    public void addi(ShareRingVector other, boolean plain) {
        SquareShareZlVector that = (SquareShareZlVector) other;
        vector.addi(that.vector);
        this.plain = plain;
    }

    @Override
    public SquareShareZlVector neg(boolean plain) {
        ZlVector resultVector = vector.neg();
        return SquareShareZlVector.create(resultVector, plain);
    }

    @Override
    public void negi(boolean plain) {
        vector.negi();
        this.plain = plain;
    }

    @Override
    public SquareShareZlVector sub(ShareRingVector other, boolean plain) {
        SquareShareZlVector that = (SquareShareZlVector) other;
        ZlVector resultVector = vector.sub(that.vector);
        return SquareShareZlVector.create(resultVector, plain);
    }

    @Override
    public void subi(ShareRingVector other, boolean plain) {
        SquareShareZlVector that = (SquareShareZlVector) other;
        vector.subi(that.vector);
        this.plain = plain;
    }

    @Override
    public SquareShareZlVector mul(ShareRingVector other) {
        SquareShareZlVector that = (SquareShareZlVector) other;
        ZlVector resultVector = vector.mul(that.vector);
        return SquareShareZlVector.create(resultVector, plain && that.plain);
    }

    @Override
    public void muli(ShareRingVector other) {
        SquareShareZlVector that = (SquareShareZlVector) other;
        vector.muli(that.vector);
        plain = plain && that.plain;
    }

    @Override
    public ZlVector getVector() {
        return vector;
    }

    public BigInteger getElement(int index) {
        return vector.getElement(index);
    }

    @Override
    public boolean isPlain() {
        return plain;
    }

    @Override
    public SquareShareZlVector split(int splitNum) {
        ZlVector splitVector = vector.split(splitNum);
        return SquareShareZlVector.create(splitVector, plain);
    }

    @Override
    public void reduce(int splitNum) {
        vector.reduce(splitNum);
    }

    @Override
    public void merge(ShareVector other) {
        SquareShareZlVector that = (SquareShareZlVector) other;
        Preconditions.checkArgument(this.plain == that.plain, "plain state mismatch");
        vector.merge(that.getVector());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(vector)
            .append(plain)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SquareShareZlVector) {
            SquareShareZlVector that = (SquareShareZlVector) obj;
            return new EqualsBuilder()
                .append(this.vector, that.vector)
                .append(this.plain, that.plain)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", plain ? "plain" : "secret", vector.toString());
    }
}
