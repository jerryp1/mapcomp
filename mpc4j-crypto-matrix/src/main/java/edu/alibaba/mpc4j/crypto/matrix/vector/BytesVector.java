package edu.alibaba.mpc4j.crypto.matrix.vector;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * bytes vector.
 *
 * @author Weiran Liu
 * @date 2023/3/31
 */
public class BytesVector {
    /**
     * bit length
     */
    private int bitLength;
    /**
     * byte length
     */
    private int byteLength;
    /**
     * bytes vector
     */
    private byte[][] bytesArray;

    /**
     * Creates a bytes vector.
     *
     * @param bitLength  the bit length.
     * @param bytesArray bytes array.
     * @return a bytes vector.
     */
    public static BytesVector create(int bitLength, byte[][] bytesArray) {
        BytesVector bytesVector = new BytesVector();
        MathPreconditions.checkPositive("bit length", bitLength);
        bytesVector.bitLength = bitLength;
        bytesVector.byteLength = CommonUtils.getByteLength(bitLength);
        bytesVector.bytesArray = Arrays.stream(bytesArray)
            .peek(bytes ->
                Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(
                    bytes, bytesVector.byteLength, bytesVector.bitLength
                ))
            )
            .toArray(byte[][]::new);
        return bytesVector;
    }

    /**
     * Creates a random bytes vector.
     *
     * @param bitLength    the bit length.
     * @param secureRandom the random state.
     * @return a random bytes vector.
     */
    public static BytesVector createRandom(int bitLength, int vectorLength, SecureRandom secureRandom) {
        BytesVector bytesVector = new BytesVector();
        MathPreconditions.checkPositive("bit length", bitLength);
        bytesVector.bitLength = bitLength;
        bytesVector.byteLength = CommonUtils.getByteLength(bitLength);
        MathPreconditions.checkPositive("vector length", vectorLength);
        bytesVector.bytesArray = IntStream.range(0, vectorLength)
            .mapToObj(index -> BytesUtils.randomByteArray(bytesVector.byteLength, bytesVector.bitLength, secureRandom))
            .toArray(byte[][]::new);
        return bytesVector;
    }

    /**
     * Creates a bytes vector by combining bytes vectors.
     *
     * @param bitLength    the bit length of the combined bytes vector.
     * @param bytesVectors the combining bytes vectors.
     * @return a bytes vector.
     */
    public static BytesVector create(int bitLength, BytesVector... bytesVectors) {
        MathPreconditions.checkPositive("# of bytes vectors", bytesVectors.length);
        int vectorLength = bytesVectors[0].getVectorLength();
        // check all bytes vectors have the same vector length
        Arrays.stream(bytesVectors).forEach(bytesVector ->
            MathPreconditions.checkEqual(
                "vector.length", "vector_length",
                bytesVector.getVectorLength(), vectorLength
            )
        );
        // combine each bytes vector
        BigInteger[] bigIntegerVector = new BigInteger[vectorLength];
        Arrays.fill(bigIntegerVector, BigInteger.ZERO);
        for (BytesVector bytesVector : bytesVectors) {
            int partitionBitLength = bytesVector.bitLength;
            for (int index = 0; index < vectorLength; index++) {
                BigInteger partitionBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(
                    bytesVector.bytesArray[index]
                );
                bigIntegerVector[index] = bigIntegerVector[index]
                    .shiftLeft(partitionBitLength)
                    .add(partitionBigInteger);
            }
        }
        // verify that all combined vectors has at most upper-bound bit length
        Arrays.stream(bigIntegerVector)
            .forEach(bigInteger -> Preconditions.checkArgument(bigInteger.bitLength() <= bitLength));
        int byteLength = CommonUtils.getByteLength(bitLength);
        byte[][] bytesArray = Arrays.stream(bigIntegerVector)
            .map(bigInteger -> BigIntegerUtils.nonNegBigIntegerToByteArray(bigInteger, byteLength))
            .toArray(byte[][]::new);
        // create the combined bytes vector
        return BytesVector.create(bitLength, bytesArray);
    }

    /**
     * Creates a bytes vector by combining bit vectors.
     *
     * @param envType    the environment.
     * @param parallel   parallel combination.
     * @param bitVectors the combining bit vectors.
     * @return a bytes vector.
     */
    public static BytesVector create(EnvType envType, boolean parallel, BitVector... bitVectors) {
        MathPreconditions.checkPositive("# of bit vectors", bitVectors.length);
        int bitLength = bitVectors.length;
        int vectorLength = bitVectors[0].bitNum();
        // check all bit vectors has the same bit num
        Arrays.stream(bitVectors).forEach(bitVector ->
            MathPreconditions.checkEqual("vectorLength", "bitVector.bitNum", vectorLength, bitVector.bitNum())
        );
        TransBitMatrix bitMatrix = TransBitMatrixFactory.createInstance(envType, vectorLength, bitLength, parallel);
        for (int columnIndex = 0; columnIndex < bitLength; columnIndex++) {
            bitMatrix.setColumn(columnIndex, bitVectors[columnIndex].getBytes());
        }
        TransBitMatrix transBitMatrix = bitMatrix.transpose();
        byte[][] bytesArray = IntStream.range(0, vectorLength)
            .mapToObj(transBitMatrix::getColumn)
            .toArray(byte[][]::new);
        // create the result
        return create(bitLength, bytesArray);
    }

    /**
     * Creates an empty bytes vector.
     *
     * @param bitLength bit length.
     * @return a bytes vector.
     */
    public static BytesVector createEmpty(int bitLength) {
        BytesVector bytesVector = new BytesVector();
        MathPreconditions.checkPositive("bit length", bitLength);
        bytesVector.bitLength = bitLength;
        bytesVector.byteLength = CommonUtils.getByteLength(bitLength);
        bytesVector.bytesArray = new byte[0][bytesVector.byteLength];

        return bytesVector;
    }

    private BytesVector() {
        // empty
    }

    /**
     * Partitions the bytes vector by the assigned partition bit length. Note that each bit length of the partitioned
     * bytes vector is the assigned partition bit length. For example, when the current bit length is 3, and the
     * partition bit length is 9, then we create 1 partition bytes vector with bit length 9 (byte length 2), but all
     * first byte in the partitioned bytes vector are 0.
     *
     * @param partitionBitLength the partition bit length.
     * @return the partition result.
     */
    public BytesVector[] partition(int partitionBitLength) {
        MathPreconditions.checkPositive("partitionBitLength", partitionBitLength);
        int partitionNum = CommonUtils.getUnitNum(bitLength, partitionBitLength);
        BytesVector[] partitionBytesVectorArray = new BytesVector[partitionNum];
        // mod = 2^l, where l is the partition bit length.
        BigInteger mod = BigInteger.ONE.shiftLeft(partitionBitLength);
        int partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        int vectorLength = getVectorLength();
        BigInteger[] bigIntegerVector = Arrays.stream(bytesArray)
            .map(BigIntegerUtils::byteArrayToNonNegBigInteger)
            .toArray(BigInteger[]::new);
        // we need to partition in reverse order so that we can then combine
        for (int partitionIndex = partitionNum - 1; partitionIndex >= 0; partitionIndex--) {
            byte[][] partitionBytesArray = new byte[vectorLength][partitionByteLength];
            for (int index = 0; index < vectorLength; index++) {
                BigInteger partitionBigInteger = bigIntegerVector[index].mod(mod);
                bigIntegerVector[index] = bigIntegerVector[index].shiftRight(partitionBitLength);
                partitionBytesArray[index] = BigIntegerUtils.nonNegBigIntegerToByteArray(partitionBigInteger, partitionByteLength);
            }
            partitionBytesVectorArray[partitionIndex] = BytesVector.create(partitionBitLength, partitionBytesArray);
        }
        return partitionBytesVectorArray;
    }

    /**
     * Partitions the bytes vector by 1 bit.
     *
     * @param envType  the environment.
     * @param parallel parallel operation.
     * @return the partition result.
     */
    public BitVector[] bitPartition(EnvType envType, boolean parallel) {
        int vectorLength = getVectorLength();
        DenseBitMatrix byteDenseBitMatrix = ByteDenseBitMatrix.fromDense(bitLength, bytesArray);
        DenseBitMatrix transByteDenseBitMatrix = byteDenseBitMatrix.transpose(envType, parallel);
        return IntStream.range(0, bitLength)
            .mapToObj(index -> BitVectorFactory.create(vectorLength, transByteDenseBitMatrix.getRow(index)))
            .toArray(BitVector[]::new);
    }

    /**
     * Gets the byte array.
     *
     * @param index the index.
     * @return the byte array.
     */
    public byte[] getBytes(int index) {
        return bytesArray[index];
    }

    /**
     * Gets the bytes array.
     *
     * @return the bytes array.
     */
    public byte[][] getBytesArray() {
        return bytesArray;
    }

    /**
     * Gets the byte length.
     *
     * @return the byte length.
     */
    public int getByteLength() {
        return byteLength;
    }

    /**
     * Gets the bit length.
     *
     * @return the bit length.
     */
    public int getBitLength() {
        return bitLength;
    }

    /**
     * Gets the vector length.
     *
     * @return the vector length.
     */
    public int getVectorLength() {
        return bytesArray.length;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        Arrays.stream(bytesArray).forEach(hashCodeBuilder::append);
        return hashCodeBuilder.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BytesVector) {
            BytesVector that = (BytesVector) obj;
            if (this.getVectorLength() != that.getVectorLength()) {
                return false;
            }
            int vectorLength = getVectorLength();
            EqualsBuilder equalsBuilder = new EqualsBuilder();
            IntStream.range(0, vectorLength)
                .forEach(index -> equalsBuilder.append(this.bytesArray[index], that.bytesArray[index]));
            return equalsBuilder.isEquals();
        }
        return false;
    }
}
