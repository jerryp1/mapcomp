package edu.alibaba.mpc4j.s2pc.aby.sbitmap.utils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.roaringbitmap.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Utilities for RoaringBitmap.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public class RoaringBitmapUtils {
    /**
     * private constructor.
     */
    private RoaringBitmapUtils() {
        // empty
    }

    /**
     * maximal byte capacity for a container
     */
    private static final int CONTAINER_MAX_BYTE_CAPACITY = BitmapContainer.MAX_CAPACITY / Byte.SIZE;

    /**
     * Check if the maximal number of bits ({@code maxBitNum}) is valid. A valid {@code maxBitNum} must > 0 and divide
     * BitmapContainer.MAX_CAPACITY.
     *
     * @return the maximal number of bits.
     * @throws IllegalArgumentException if the number of bits is invalid.
     */
    @CanIgnoreReturnValue
    public static int checkValidMaxBitNum(int maxBitNum) {
        MathPreconditions.checkPositive("maxBitNum", maxBitNum);
        if ((maxBitNum & (BitmapContainer.MAX_CAPACITY - 1)) != 0) {
            throw new IllegalArgumentException("maxBitNum (" + maxBitNum + ") must divide " + BitmapContainer.MAX_CAPACITY);
        }
        return maxBitNum;
    }

    /**
     * Returns the number of containers used for storing the number of bits. This method does not check if
     * {@code bitNum} is valid.
     *
     * @param bitNum the number of bits.
     * @return the number of containers used for storing the number of bits.
     */
    public static int getContainerNum(int bitNum) {
        return CommonUtils.getUnitNum(bitNum, BitmapContainer.MAX_CAPACITY);
    }

    /**
     * Returns the number of bytes used for storing the number of bits. This method does not check if {@code bitNum} is
     * valid.
     *
     * @param bitNum the number of bits.
     * @return the number of bytes used for storing the number of bits.
     */
    public static int getByteNum(int bitNum) {
        return CommonUtils.getByteLength(bitNum);
    }

    /**
     * Check if the bitmap contains valid bits, that is, the bitmap does not contain elements larger than totalBitNum.
     *
     * @param totalBitNum the total number of bits.
     * @param bitmap      the given bitmap.
     * @throws IllegalArgumentException if the bitmap contains elements larger than totalBitNum.
     */
    public static void checkContainValidBits(int totalBitNum, RoaringBitmap bitmap) {
        if (!bitmap.isEmpty()) {
            MathPreconditions.checkNonNegative("first element", bitmap.first());
            MathPreconditions.checkLessThan("last element", bitmap.last(), totalBitNum);
        }
    }

    /**
     * Expand the bitmap to a whole bit vector, fill all-zero values for the missing keys.
     *
     * @param totalBitNum   the total number of bits.
     * @param roaringBitmap the given bitmap.
     * @return the resulting bit vector.
     * @throws IllegalArgumentException if {@code totalBitNum} is not positive, or if bitmap contains elements larger
     *                                  than {@code totalBitNum}.
     */
    public static BitVector toBitVector(int totalBitNum, RoaringBitmap roaringBitmap) {
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        checkContainValidBits(totalBitNum, roaringBitmap);
        int totalContainerNum = getContainerNum(totalBitNum);
        if (roaringBitmap.isEmpty()) {
            // empty RoaringBitmap, create an all-zero BitVector.
            return BitVectorFactory.createZeros(totalBitNum);
        }
        // expend the RoaringBitmap, fill BitmapContainer with all-zero values for the missing keys.
        ContainerPointer containerPointer = roaringBitmap.getContainerPointer();
        // create an BitmapContainer array that stores maximal number of bitmapContainers.
        // Note that we must use BitmapContainer, since other Containers.writeArray() would write compressed format.
        BitmapContainer[] bitmapContainers = new BitmapContainer[totalContainerNum];
        // iteratively assign the BitmapContainer
        while (containerPointer.getContainer() != null) {
            int key = containerPointer.key();
            assert key < totalContainerNum : "key must be less than " + totalContainerNum + ": " + key;
            bitmapContainers[key] = containerPointer.getContainer().toBitmapContainer();
            containerPointer.advance();
        }
        // fill all-zero BitmapContainers for the missing keys.
        for (int containerIndex = 0; containerIndex < totalContainerNum; containerIndex++) {
            if (bitmapContainers[containerIndex] == null) {
                bitmapContainers[containerIndex] = new BitmapContainer();
            }
        }
        // merge bitmapContainers to get the Bit Vector.
        // we first create a rounded ByteBuffer, then reduce the length to be the minimal totalByteNum.
        int totalRoundByteNum = totalContainerNum * RoaringBitmapUtils.CONTAINER_MAX_BYTE_CAPACITY;
        ByteBuffer byteBuffer = ByteBuffer.allocate(totalRoundByteNum).order(ByteOrder.LITTLE_ENDIAN);
        Arrays.stream(bitmapContainers).forEach(bitmapContainer -> bitmapContainer.writeArray(byteBuffer));
        byte[] roundBytes = byteBuffer.array();
        int totalByteNum = RoaringBitmapUtils.getByteNum(totalBitNum);
        if (roundBytes.length == totalByteNum) {
            return BitVectorFactory.create(totalBitNum, roundBytes);
        } else {
            return BitVectorFactory.create(totalBitNum, Arrays.copyOf(roundBytes, totalByteNum));
        }
    }

    /**
     * Compress the bit vector to a bitmap.
     *
     * @param bitVector the given bit vector.
     * @return the resulting bitmap.
     */
    public static RoaringBitmap toRoaringBitmap(BitVector bitVector) {
        int totalBitNum = bitVector.bitNum();
        int totalContainerNum = getContainerNum(totalBitNum);
        BitVector copyBitVector = bitVector.copy();
        int offsetBitNum = totalContainerNum * BitmapContainer.MAX_CAPACITY - totalBitNum;
        if (offsetBitNum > 0) {
            copyBitVector.merge(BitVectorFactory.createZeros(offsetBitNum));
        }
        long[][] containerLongArrays = new long[totalContainerNum][];
        for (int containerIndex = 0; containerIndex < totalContainerNum; containerIndex++) {
            BitVector containerBitVector = copyBitVector.split(BitmapContainer.MAX_CAPACITY);
            byte[] containerBytes = containerBitVector.getBytes();
            containerLongArrays[containerIndex] = LongUtils.byteArrayToLongArray(containerBytes, ByteOrder.LITTLE_ENDIAN);
        }
        RoaringBitmap bitmap = new RoaringBitmap();
        // obtain cardinality for each bitmapLongArray
        int[] cardinalities = Arrays.stream(containerLongArrays)
            .mapToInt(containerLongArray -> Arrays.stream(containerLongArray).mapToInt(Long::bitCount).sum())
            .toArray();
        for (int key = 0; key < totalContainerNum; key++) {
            if (cardinalities[key] == 0) {
                // ignore empty container
                continue;
            }
            Container container = new BitmapContainer(containerLongArrays[key], cardinalities[key]);
            // repair container for suitable storage mode
            container.repairAfterLazy();
            bitmap.append((char) key, container);
        }
        // do run optimize to obtain the best RoaringBitMap
        bitmap.runOptimize();
        return bitmap;
    }

    /**
     * Returns the key array (represented in char[]) stored in the bitmap.
     *
     * @param bitmap the given bitmap.
     * @return the key array.
     */
    public static char[] getKeyCharArray(RoaringBitmap bitmap) {
        if (bitmap.isEmpty()) {
            // empty RoaringBitmap, create a 0-length char array
            return new char[0];
        }
        ContainerPointer containerPointer = bitmap.getContainerPointer();
        // first round, decide the number of containers
        ContainerPointer containerNumPointer = containerPointer.clone();
        int containerNum = 0;
        while (containerNumPointer.getContainer() != null) {
            containerNum++;
            containerNumPointer.advance();
        }
        char[] keyCharArray = new char[containerNum];
        // second round, get all keys
        ContainerPointer containerKeyPointer = containerPointer.clone();
        int counter = 0;
        while (containerKeyPointer.getContainer() != null) {
            keyCharArray[counter] = containerKeyPointer.key();
            counter++;
            containerKeyPointer.advance();
        }
        return keyCharArray;
    }

    /**
     * Returns the key array (represented in int[]) stored in the bitmap.
     *
     * @param bitmap the given bitmap.
     * @return the key array.
     */
    public static int[] getKeyIntArray(RoaringBitmap bitmap) {
        if (bitmap.isEmpty()) {
            // empty RoaringBitmap, create a 0-length char array
            return new int[0];
        }
        ContainerPointer containerPointer = bitmap.getContainerPointer();
        // first round, decide the number of containers
        ContainerPointer containerNumPointer = containerPointer.clone();
        int containerNum = 0;
        while (containerNumPointer.getContainer() != null) {
            containerNum++;
            containerNumPointer.advance();
        }
        int[] keyIntArray = new int[containerNum];
        // second round, get all keys
        ContainerPointer containerKeyPointer = containerPointer.clone();
        int counter = 0;
        while (containerKeyPointer.getContainer() != null) {
            keyIntArray[counter] = containerKeyPointer.key();
            counter++;
            containerKeyPointer.advance();
        }
        return keyIntArray;
    }

    /**
     * Expand the bitmap to bit vectors with roaring format.
     *
     * @param totalBitNum the total number of bits.
     * @param bitmap      the given bitmap.
     * @return the bit vectors with roaring format.
     * @throws IllegalArgumentException if {@code totalBitNum} is not positive, or if bitmap contains elements larger
     *                                  than {@code totalBitNum}.
     */
    public static BitVector[] toRoaringBitVectors(int totalBitNum, RoaringBitmap bitmap) {
        MathPreconditions.checkPositive("totalBitNum", totalBitNum);
        checkContainValidBits(totalBitNum, bitmap);
        int totalContainerNum = getContainerNum(totalBitNum);
        if (bitmap.isEmpty()) {
            // empty RoaringBitmap, create a 0-length BitVector array
            return new BitVector[0];
        }
        ContainerPointer containerPointer = bitmap.getContainerPointer();
        // first round, decide the number of containers
        ContainerPointer containerNumPointer = containerPointer.clone();
        int containerNum = 0;
        while (containerNumPointer.getContainer() != null) {
            containerNum++;
            containerNumPointer.advance();
        }
        // second round, expend each bitmapContainer to a bit vector.
        ContainerPointer containerDataPointer = containerPointer.clone();
        // create an array that stores maximal number of containers.
        BitVector[] bitVectors = new BitVector[containerNum];
        // iteratively assign the BitmapContainer
        int counter = 0;
        while (containerDataPointer.getContainer() != null) {
            int key = containerDataPointer.key();
            assert key < totalContainerNum : "key must be less than " + totalContainerNum + ": " + key;
            // Note that we must use BitmapContainer, since other Containers.writeArray() would write compressed format.
            BitmapContainer bitmapContainer = containerDataPointer.getContainer().toBitmapContainer();
            ByteBuffer byteBuffer = ByteBuffer.allocate(CONTAINER_MAX_BYTE_CAPACITY).order(ByteOrder.LITTLE_ENDIAN);
            bitmapContainer.writeArray(byteBuffer);
            bitVectors[counter] = BitVectorFactory.create(BitmapContainer.MAX_CAPACITY, byteBuffer.array());
            counter++;
            containerDataPointer.advance();
        }
        return bitVectors;
    }

    /**
     * Compress the bit vectors with roaring format to a RoaringBitmap.
     *
     * @param bitVectors bit vectors with roaring format.
     * @return the resulting RoaringBitmap.
     * @throws IllegalArgumentException if key num does not match the vector length, or the number of bits contained in
     *                                  each bit vector is invalid.
     */
    public static RoaringBitmap toRoaringBitmap(char[] keys, BitVector[] bitVectors) {
        MathPreconditions.checkEqual("keys.length", "vectors.length", keys.length, bitVectors.length);
        int containerNum = keys.length;
        long[][] containerLongArrays = new long[containerNum][];
        for (int containerIndex = 0; containerIndex < containerNum; containerIndex++) {
            MathPreconditions.checkEqual(
                "vector.bitNum", "BitmapContainer.MAX_CAPACITY",
                bitVectors[containerIndex].bitNum(), BitmapContainer.MAX_CAPACITY
            );
            byte[] containerBytes = bitVectors[containerIndex].getBytes();
            containerLongArrays[containerIndex] = LongUtils.byteArrayToLongArray(containerBytes, ByteOrder.LITTLE_ENDIAN);
        }
        RoaringBitmap roaringBitmap = new RoaringBitmap();
        // obtain cardinality for each bitmapLongArray
        int[] cardinalities = Arrays.stream(containerLongArrays)
            .mapToInt(containerLongArray -> Arrays.stream(containerLongArray).mapToInt(Long::bitCount).sum())
            .toArray();
        for (int index = 0; index < containerNum; index++) {
            if (cardinalities[index] == 0) {
                // ignore empty container
                continue;
            }
            Container container = new BitmapContainer(containerLongArrays[index], cardinalities[index]);
            // repair container for suitable storage mode
            container.repairAfterLazy();
            roaringBitmap.append(keys[index], container);
        }
        // do run optimize to obtain the best RoaringBitMap
        roaringBitmap.runOptimize();
        return roaringBitmap;
    }
}
