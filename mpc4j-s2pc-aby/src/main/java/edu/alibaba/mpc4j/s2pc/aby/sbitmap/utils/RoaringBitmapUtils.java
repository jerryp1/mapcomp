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
     * Check if the number of bits ({@code bitNum}) is valid. A valid {@code bitNum} must > 0 and divide
     * BitmapContainer.MAX_CAPACITY.
     *
     * @return the number of bits.
     * @throws IllegalArgumentException if the number of bits is invalid.
     */
    @CanIgnoreReturnValue
    public static int checkValidBitNum(int bitNum) {
        MathPreconditions.checkPositive("bitNum", bitNum);
        if ((bitNum & (BitmapContainer.MAX_CAPACITY - 1)) != 0) {
            throw new IllegalArgumentException("bitNum (" + bitNum + ") must divide " + BitmapContainer.MAX_CAPACITY);
        }
        return bitNum;
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
     * Expand the RoaringBitmap to a whole bit vector, fill all-zero values for the missing keys.
     *
     * @param maxBitNum     the given maximal number of bits.
     * @param roaringBitmap the given RoaringBitmap.
     * @return the resulting BitVector.
     * @throws IllegalArgumentException if {@code maxBitNum} is invalid.
     */
    public static BitVector toBitVector(int maxBitNum, RoaringBitmap roaringBitmap) {
        checkValidBitNum(maxBitNum);
        int maxContainerNum = getContainerNum(maxBitNum);
        if (roaringBitmap.isEmpty()) {
            // empty RoaringBitmap, create an all-zero BitVector.
            return BitVectorFactory.createZeros(maxBitNum);
        }
        // expend the RoaringBitmap, fill BitmapContainer with all-zero values for the missing keys.
        ContainerPointer containerPointer = roaringBitmap.getContainerPointer();
        // create an BitmapContainer array that stores maximal number of bitmapContainers.
        // Note that we must use BitmapContainer, since other Containers.writeArray() would write compressed format.
        BitmapContainer[] bitmapContainers = new BitmapContainer[maxContainerNum];
        // iteratively assign the BitmapContainer
        while (containerPointer.getContainer() != null) {
            int key = containerPointer.key();
            MathPreconditions.checkLessThan("key", key, maxContainerNum);
            bitmapContainers[key] = containerPointer.getContainer().toBitmapContainer();
            containerPointer.advance();
        }
        // fill all-zero BitmapContainers for the missing keys.
        for (int containerIndex = 0; containerIndex < maxContainerNum; containerIndex++) {
            if (bitmapContainers[containerIndex] == null) {
                bitmapContainers[containerIndex] = new BitmapContainer();
            }
        }
        // merge bitmapContainers to get the Bit Vector.
        int maxByteNum = getByteNum(maxBitNum);
        ByteBuffer byteBuffer = ByteBuffer.allocate(maxByteNum).order(ByteOrder.LITTLE_ENDIAN);
        Arrays.stream(bitmapContainers).forEach(bitmapContainer -> bitmapContainer.writeArray(byteBuffer));

        return BitVectorFactory.create(maxBitNum, byteBuffer.array());
    }

    /**
     * Compress the BitVector to a RoaringBitmap.
     *
     * @param bitVector the given BitVector.
     * @return the resulting RoaringBitmap.
     * @throws IllegalArgumentException if the number of bits contained in the BitVector is invalid.
     */
    public static RoaringBitmap toRoaringBitmap(BitVector bitVector) {
        int bitNum = bitVector.bitNum();
        checkValidBitNum(bitNum);
        int containerNum = getContainerNum(bitNum);
        BitVector copyBitVector = bitVector.copy();
        long[][] containerLongArrays = new long[containerNum][];
        for (int containerIndex = 0; containerIndex < containerNum; containerIndex++) {
            BitVector containerBitVector = copyBitVector.split(BitmapContainer.MAX_CAPACITY);
            byte[] containerBytes = containerBitVector.getBytes();
            containerLongArrays[containerIndex] = LongUtils.byteArrayToLongArray(containerBytes, ByteOrder.LITTLE_ENDIAN);
        }
        RoaringBitmap roaringBitmap = new RoaringBitmap();
        // obtain cardinality for each bitmapLongArray
        int[] cardinalities = Arrays.stream(containerLongArrays)
            .mapToInt(containerLongArray -> Arrays.stream(containerLongArray).mapToInt(Long::bitCount).sum())
            .toArray();
        for (int key = 0; key < containerNum; key++) {
            if (cardinalities[key] == 0) {
                // ignore empty container
                continue;
            }
            Container container = new BitmapContainer(containerLongArrays[key], cardinalities[key]);
            // repair container for suitable storage mode
            container.repairAfterLazy();
            roaringBitmap.append((char) key, container);
        }
        // do run optimize to obtain the best RoaringBitMap
        roaringBitmap.runOptimize();
        return roaringBitmap;
    }

    /**
     * Returns the key array (represented in char[]) stored in the RoaringBitmap.
     *
     * @param roaringBitmap the given RoaringBitmap.
     * @return the key array.
     */
    public static char[] getKeyCharArray(RoaringBitmap roaringBitmap) {
        if (roaringBitmap.isEmpty()) {
            // empty RoaringBitmap, create a 0-length char array
            return new char[0];
        }
        ContainerPointer containerPointer = roaringBitmap.getContainerPointer();
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
     * Returns the key array (represented in int[]) stored in the RoaringBitmap.
     *
     * @param roaringBitmap the given RoaringBitmap.
     * @return the key array.
     */
    public static int[] getKeyIntArray(RoaringBitmap roaringBitmap) {
        if (roaringBitmap.isEmpty()) {
            // empty RoaringBitmap, create a 0-length char array
            return new int[0];
        }
        ContainerPointer containerPointer = roaringBitmap.getContainerPointer();
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
     * Expand the RoaringBitmap to bit vectors with roaring format.
     *
     * @param maxBitNum     the given maximal number of bits.
     * @param roaringBitmap the given RoaringBitmap.
     * @return the bit vectors with roaring format.
     * @throws IllegalArgumentException if {@code maxBitNum} is invalid.
     */
    public static BitVector[] toRoaringBitVectors(int maxBitNum, RoaringBitmap roaringBitmap) {
        checkValidBitNum(maxBitNum);
        int maxContainerNum = getContainerNum(maxBitNum);
        if (roaringBitmap.isEmpty()) {
            // empty RoaringBitmap, create a 0-length BitVector array
            return new BitVector[0];
        }
        ContainerPointer containerPointer = roaringBitmap.getContainerPointer();
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
            MathPreconditions.checkLessThan("key", key, maxContainerNum);
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
     * each bit vector is invalid.
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
