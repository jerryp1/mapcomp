package edu.alibaba.mpc4j.crypto.matrix.vector;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.MatrixUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * the Zl vector.
 *
 * @author Weiran Liu
 * @date 2023/4/10
 */
public class ZlVector implements RingVector {
    /**
     * merges vectors.
     *
     * @param vectors vectors.
     * @return the merged vector.
     */
    public static ZlVector merge(ZlVector[] vectors) {
        MathPreconditions.checkPositive("vectors.length", vectors.length);
        int len = Arrays.stream(vectors).mapToInt(ZlVector::getNum).sum();
        BigInteger[] mergeElements = new BigInteger[len];
        for (int i = 0, pos = 0; i < vectors.length; i++) {
            Preconditions.checkArgument(vectors[i].zl.equals(vectors[0].getZl()));
            MathPreconditions.checkPositive("vector.num", vectors[i].getNum());
            System.arraycopy(vectors[i].elements, 0, mergeElements, pos, vectors[i].elements.length);
            pos += vectors[i].elements.length;
        }
        return ZlVector.create(vectors[0].getZl(), mergeElements);
    }

    /**
     * splits the vector.
     *
     * @param mergeVector the merged vector.
     * @param nums        nums for each of the split vector.
     * @return the split vectors.
     */
    public static ZlVector[] split(ZlVector mergeVector, int[] nums) {
        int num = mergeVector.getNum();
        MathPreconditions.checkEqual("sum(nums)", "mergeVector.getNum()", Arrays.stream(nums).sum(), num);
        BigInteger[][] spRes = new BigInteger[nums.length][];
        for (int i = 0, startPos = 0; i < nums.length; i++) {
            spRes[i] = Arrays.copyOfRange(mergeVector.elements, startPos, startPos + nums[i]);
            startPos += nums[i];
        }
        return Arrays.stream(spRes).map(x -> ZlVector.create(mergeVector.getZl(), x)).toArray(ZlVector[]::new);
    }

    /**
     * merges vectors, and fix the length of each vector into (ByteLength(bitNum))<<3
     *
     * @param vectors vectors.
     * @return the merged vector.
     */
    public static ZlVector mergeWithPadding(ZlVector[] vectors) {
        MathPreconditions.checkPositive("vectors.length", vectors.length);
        int len = Arrays.stream(vectors).mapToInt(x -> CommonUtils.getByteLength(x.getNum()) << 3).sum();
        BigInteger[] mergeElements = new BigInteger[len];
        for (int i = 0, pos = 0; i < vectors.length; i++) {
            int partNum = vectors[i].elements.length & 7;
            if(partNum > 0){
                for(int j = 0; j < 8 - partNum; j++){
                    mergeElements[pos++] = BigInteger.ZERO;
                }
            }
            Preconditions.checkArgument(vectors[i].zl.equals(vectors[0].getZl()));
            MathPreconditions.checkPositive("vector.num", vectors[i].getNum());
            System.arraycopy(vectors[i].elements, 0, mergeElements, pos, vectors[i].elements.length);
            pos += vectors[i].elements.length;
        }
        return ZlVector.create(vectors[0].getZl(), mergeElements);
    }

    /**
     * splits the vector, where the length of each vector in mergeVector is fixed into (ByteLength(bitNum))<<3
     *
     * @param mergeVector the merged vector.
     * @param nums        nums for each of the split vector.
     * @return the split vectors.
     */
    public static ZlVector[] splitWithPadding(ZlVector mergeVector, int[] nums) {
        int num = mergeVector.getNum();
        MathPreconditions.checkEqual("sum(nums)", "mergeVector.getNum()",
            Arrays.stream(nums).map(x -> CommonUtils.getByteLength(x) << 3).sum(), num);
        BigInteger[][] spRes = new BigInteger[nums.length][];
        for (int i = 0, startPos = 0; i < nums.length; i++) {
            int partNum = nums[i] & 7;
            startPos += partNum > 0 ? 8 - partNum : 0;
            spRes[i] = Arrays.copyOfRange(mergeVector.elements, startPos, startPos + nums[i]);
            startPos += nums[i];
        }
        return Arrays.stream(spRes).map(x -> ZlVector.create(mergeVector.getZl(), x)).toArray(ZlVector[]::new);
    }

    /**
     * Creates a vector.
     *
     * @param zl       Zl instance.
     * @param elements elements.
     * @return a vector.
     */
    public static ZlVector create(Zl zl, BigInteger[] elements) {
        ZlVector vector = new ZlVector(zl);
        MathPreconditions.checkPositive("num", elements.length);
        vector.elements = Arrays.stream(elements)
            .peek(element -> Preconditions.checkArgument(zl.validateElement(element)))
            .toArray(BigInteger[]::new);
        return vector;
    }

    /**
     * Creates a random vector.
     *
     * @param zl           Zl instance.
     * @param num          the num.
     * @param secureRandom the random state.
     * @return a vector.
     */
    public static ZlVector createRandom(Zl zl, int num, SecureRandom secureRandom) {
        ZlVector vector = new ZlVector(zl);
        MathPreconditions.checkPositive("num", num);
        vector.elements = IntStream.range(0, num)
            .mapToObj(index -> zl.createRandom(secureRandom))
            .toArray(BigInteger[]::new);
        return vector;
    }

    /**
     * Creates an all-one vector.
     *
     * @param zl  Zl instance.
     * @param num the num.
     * @return a vector.
     */
    public static ZlVector createOnes(Zl zl, int num) {
        ZlVector vector = new ZlVector(zl);
        MathPreconditions.checkPositive("num", num);
        vector.elements = IntStream.range(0, num)
            .mapToObj(index -> zl.createOne())
            .toArray(BigInteger[]::new);
        return vector;
    }

    /**
     * Creates an all-zero vector.
     *
     * @param zl  Zl instance.
     * @param num the num.
     * @return a vector.
     */
    public static ZlVector createZeros(Zl zl, int num) {
        ZlVector vector = new ZlVector(zl);
        MathPreconditions.checkPositive("num", num);
        vector.elements = IntStream.range(0, num)
            .mapToObj(index -> zl.createZero())
            .toArray(BigInteger[]::new);
        return vector;
    }

    /**
     * Creates an empty vector.
     *
     * @param zl Zl instance.
     * @return a vector.
     */
    public static ZlVector createEmpty(Zl zl) {
        ZlVector vector = new ZlVector(zl);
        vector.elements = new BigInteger[0];

        return vector;
    }

    /**
     * Zl instance
     */
    private final Zl zl;
    /**
     * elements
     */
    private BigInteger[] elements;
    /**
     * parallel operation.
     */
    private boolean parallel;

    private ZlVector(Zl zl) {
        this.zl = zl;
    }

    @Override
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    @Override
    public ZlVector copy() {
        BigInteger[] copyElements = BigIntegerUtils.clone(elements);
        return ZlVector.create(zl, copyElements);
    }

    @Override
    public void replaceCopy(Vector other) {
        ZlVector that = (ZlVector) other;
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
        int num = getNum();
        System.arraycopy(that.elements, 0, this.elements, 0, num);
    }

    @Override
    public int getNum() {
        return elements.length;
    }

    @Override
    public ZlVector split(int splitNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("splitNum", splitNum, num);
        BigInteger[] subElements = new BigInteger[splitNum];
        BigInteger[] remainElements = new BigInteger[num - splitNum];
        System.arraycopy(elements, 0, subElements, 0, splitNum);
        System.arraycopy(elements, splitNum, remainElements, 0, num - splitNum);
        elements = remainElements;
        return ZlVector.create(zl, subElements);
    }

    @Override
    public void reduce(int reduceNum) {
        int num = getNum();
        MathPreconditions.checkPositiveInRangeClosed("reduceNum", reduceNum, num);
        if (reduceNum < num) {
            // reduce if the reduced rows is less than rows.
            BigInteger[] remainElements = new BigInteger[reduceNum];
            System.arraycopy(elements, 0, remainElements, 0, reduceNum);
            elements = remainElements;
        }
    }

    @Override
    public void merge(Vector other) {
        ZlVector that = (ZlVector) other;
        Preconditions.checkArgument(this.zl.equals(that.zl));
        BigInteger[] mergeElements = new BigInteger[this.elements.length + that.elements.length];
        System.arraycopy(this.elements, 0, mergeElements, 0, this.elements.length);
        System.arraycopy(that.elements, 0, mergeElements, this.elements.length, that.elements.length);
        elements = mergeElements;
    }

    @Override
    public ZlVector add(RingVector other) {
        ZlVector that = (ZlVector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        BigInteger[] results = indexIntStream
            .mapToObj(index -> zl.add(this.elements[index], that.elements[index]))
            .toArray(BigInteger[]::new);
        return ZlVector.create(zl, results);
    }

    @Override
    public void addi(RingVector other) {
        ZlVector that = (ZlVector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> this.elements[index] = zl.add(this.elements[index], that.elements[index]));
    }

    @Override
    public ZlVector neg() {
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        BigInteger[] results = indexIntStream
            .mapToObj(index -> zl.neg(elements[index]))
            .toArray(BigInteger[]::new);
        return ZlVector.create(zl, results);
    }

    @Override
    public void negi() {
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> elements[index] = zl.neg(elements[index]));
    }

    @Override
    public ZlVector sub(RingVector other) {
        ZlVector that = (ZlVector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        BigInteger[] results = indexIntStream
            .mapToObj(index -> zl.sub(this.elements[index], that.elements[index]))
            .toArray(BigInteger[]::new);
        return ZlVector.create(zl, results);
    }

    @Override
    public void subi(RingVector other) {
        ZlVector that = (ZlVector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> this.elements[index] = zl.sub(this.elements[index], that.elements[index]));
    }

    @Override
    public ZlVector mul(RingVector other) {
        ZlVector that = (ZlVector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        BigInteger[] results = indexIntStream
            .mapToObj(index -> zl.mul(this.elements[index], that.elements[index]))
            .toArray(BigInteger[]::new);
        return ZlVector.create(zl, results);
    }

    @Override
    public void muli(RingVector other) {
        ZlVector that = (ZlVector) other;
        checkInputs(that);
        int num = getNum();
        IntStream indexIntStream = IntStream.range(0, num);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> this.elements[index] = zl.mul(this.elements[index], that.elements[index]));
    }

    private void checkInputs(ZlVector that) {
        Preconditions.checkArgument(this.zl.equals(that.zl));
        MathPreconditions.checkEqual("this.num", "that.num", this.getNum(), that.getNum());
    }

    /**
     * Gets Zl instance.
     *
     * @return Zl instance.
     */
    public Zl getZl() {
        return zl;
    }

    /**
     * Gets the element.
     *
     * @param index the index.
     * @return the element.
     */
    public BigInteger getElement(int index) {
        return elements[index];
    }

    /**
     * Gets the elements.
     *
     * @return the elements.
     */
    public BigInteger[] getElements() {
        return elements;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(zl)
            .append(elements)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ZlVector) {
            ZlVector that = (ZlVector) obj;
            if (this.getNum() != that.getNum()) {
                return false;
            }
            return new EqualsBuilder()
                .append(this.zl, that.zl)
                .append(this.elements, that.elements)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        String[] stringData = Arrays.stream(Arrays.copyOf(elements, Math.min(elements.length, MatrixUtils.DISPLAY_NUM)))
            .map(BigInteger::toString)
            .toArray(String[]::new);
        return this.getClass().getSimpleName() + " (l = " + zl.getL() + "): " + Arrays.toString(stringData);
    }
}
