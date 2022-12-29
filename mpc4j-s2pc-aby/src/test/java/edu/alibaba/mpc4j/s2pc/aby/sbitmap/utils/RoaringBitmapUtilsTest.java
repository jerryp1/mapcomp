package edu.alibaba.mpc4j.s2pc.aby.sbitmap.utils;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.sbitmap.RoaringBitmapTestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.roaringbitmap.BitmapContainer;
import org.roaringbitmap.RoaringBitmap;

import java.util.Random;

/**
 * tests for RoaringBitmap utilities.
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public class RoaringBitmapUtilsTest {
    /**
     * default max number of bits
     */
    private static final int MAX_BIT_NUM = 1000000;
    /**
     * default max number of containers
     */
    private static final int MAX_CONTAINER_NUM = RoaringBitmapUtils.getContainerNum(MAX_BIT_NUM);
    /**
     * default max number of bits rounding to divide BitmapContainer.MAX_CAPACITY
     */
    private static final int MAX_ROUND_CONTAINER_CAPACITY_BIT_NUM = MAX_CONTAINER_NUM * BitmapContainer.MAX_CAPACITY;

    @Test
    public void testCheckValidBitNum() {
        // -1 is not a valid bitNum
        Assert.assertThrows(IllegalArgumentException.class, () -> RoaringBitmapUtils.checkValidBitNum(-1));
        // 0 is not a valid bitNum
        Assert.assertThrows(IllegalArgumentException.class, () -> RoaringBitmapUtils.checkValidBitNum(0));
        // 1 is not a valid bitNum
        Assert.assertThrows(IllegalArgumentException.class, () -> RoaringBitmapUtils.checkValidBitNum(1));
        // BitmapContainer.MAX_CAPACITY - 1 is not a valid bitNum
        Assert.assertThrows(IllegalArgumentException.class, () ->
            RoaringBitmapUtils.checkValidBitNum(BitmapContainer.MAX_CAPACITY - 1)
        );
        // BitmapContainer.MAX_CAPACITY is a valid bitNum
        RoaringBitmapUtils.checkValidBitNum(BitmapContainer.MAX_CAPACITY);
        // BitmapContainer.MAX_CAPACITY + 1 is not a valid bitNum
        Assert.assertThrows(IllegalArgumentException.class, () ->
            RoaringBitmapUtils.checkValidBitNum(BitmapContainer.MAX_CAPACITY + 1)
        );
        // BitmapContainer.MAX_CAPACITY * 2 - 1 is not a valid bitNum
        Assert.assertThrows(IllegalArgumentException.class, () ->
            RoaringBitmapUtils.checkValidBitNum(BitmapContainer.MAX_CAPACITY * 2 - 1)
        );
        // BitmapContainer.MAX_CAPACITY * 2 is a valid bitNum
        RoaringBitmapUtils.checkValidBitNum(BitmapContainer.MAX_CAPACITY);
        // BitmapContainer.MAX_CAPACITY * 2 + 1 is not a valid bitNum
        Assert.assertThrows(IllegalArgumentException.class, () ->
            RoaringBitmapUtils.checkValidBitNum(BitmapContainer.MAX_CAPACITY * 2 + 1)
        );
    }

    @Test
    public void testEmptyRoaringBitmapToBitVector() {
        // new RoaringBitmap
        RoaringBitmap newBitmap0 = new RoaringBitmap();
        BitVector newBitVector = RoaringBitmapUtils.toBitVector(MAX_ROUND_CONTAINER_CAPACITY_BIT_NUM, newBitmap0);
        RoaringBitmap newBitmap1 = RoaringBitmapUtils.toRoaringBitmap(newBitVector);
        Assert.assertEquals(newBitmap0, newBitmap1);
        // create RoaringBitmap with 0-length int array
        RoaringBitmap zeroArrayBitmap0 = RoaringBitmap.bitmapOf();
        BitVector zeroArrayBitVector = RoaringBitmapUtils.toBitVector(MAX_ROUND_CONTAINER_CAPACITY_BIT_NUM, zeroArrayBitmap0);
        RoaringBitmap zeroArrayBitmap1 = RoaringBitmapUtils.toRoaringBitmap(zeroArrayBitVector);
        Assert.assertEquals(zeroArrayBitmap0, zeroArrayBitmap1);
    }

    @Test
    public void testRandomRoaringBitmapToBitVector() {
        RoaringBitmap randomBitmap0 = getRandomRoaringBitmap();
        BitVector bitVector = RoaringBitmapUtils.toBitVector(MAX_ROUND_CONTAINER_CAPACITY_BIT_NUM, randomBitmap0);
        RoaringBitmap randomBitmap1 = RoaringBitmapUtils.toRoaringBitmap(bitVector);
        Assert.assertEquals(randomBitmap0, randomBitmap1);
    }

    @Test
    public void testFullRoaringBitmapToBitVector() {
        RoaringBitmap fullBitmap0 = RoaringBitmap.bitmapOfRange(0, MAX_BIT_NUM);
        BitVector fullBitVector = RoaringBitmapUtils.toBitVector(MAX_ROUND_CONTAINER_CAPACITY_BIT_NUM, fullBitmap0);
        RoaringBitmap fullBitmap1 = RoaringBitmapUtils.toRoaringBitmap(fullBitVector);
        Assert.assertEquals(fullBitmap0, fullBitmap1);
    }

    @Test
    public void testEmptyRoaringBitmapToRoaringBitVectors() {
        // new RoaringBitmap
        RoaringBitmap newBitmap0 = new RoaringBitmap();
        char[] newKeys = RoaringBitmapUtils.getKeyCharArray(newBitmap0);
        BitVector[] newRoaringBitVectors = RoaringBitmapUtils.toRoaringBitVectors(
            MAX_ROUND_CONTAINER_CAPACITY_BIT_NUM, newBitmap0
        );
        RoaringBitmap newBitmap1 = RoaringBitmapUtils.toRoaringBitmap(newKeys, newRoaringBitVectors);
        Assert.assertEquals(newBitmap0, newBitmap1);
        // create RoaringBitmap with 0-length int array
        RoaringBitmap zeroArrayBitmap0 = RoaringBitmap.bitmapOf();
        char[] zeroArrayKeys = RoaringBitmapUtils.getKeyCharArray(zeroArrayBitmap0);
        BitVector[] zeroArrayRoaringBitVectors = RoaringBitmapUtils.toRoaringBitVectors(
            MAX_ROUND_CONTAINER_CAPACITY_BIT_NUM, zeroArrayBitmap0
        );
        RoaringBitmap zeroArrayBitmap1 = RoaringBitmapUtils.toRoaringBitmap(zeroArrayKeys, zeroArrayRoaringBitVectors);
        Assert.assertEquals(zeroArrayBitmap0, zeroArrayBitmap1);
    }

    @Test
    public void testRandomRoaringBitmapToRoaringBitVectors() {
        RoaringBitmap randomBitmap0 = getRandomRoaringBitmap();
        char[] randomKeys = RoaringBitmapUtils.getKeyCharArray(randomBitmap0);
        BitVector[] randomRoaringBitVectors = RoaringBitmapUtils.toRoaringBitVectors(
            MAX_ROUND_CONTAINER_CAPACITY_BIT_NUM, randomBitmap0
        );
        RoaringBitmap randomBitmap1 = RoaringBitmapUtils.toRoaringBitmap(randomKeys, randomRoaringBitVectors);
        Assert.assertEquals(randomBitmap0, randomBitmap1);
    }

    @Test
    public void testFullRoaringBitmapToRoaringBitVectors() {
        RoaringBitmap fullBitmap0 = RoaringBitmap.bitmapOfRange(0, MAX_BIT_NUM);
        char[] fullKeys = RoaringBitmapUtils.getKeyCharArray(fullBitmap0);
        BitVector[] fullRoaringBitVectors = RoaringBitmapUtils.toRoaringBitVectors(
            MAX_ROUND_CONTAINER_CAPACITY_BIT_NUM, fullBitmap0
        );
        RoaringBitmap fullBitmap1 = RoaringBitmapUtils.toRoaringBitmap(fullKeys, fullRoaringBitVectors);
        Assert.assertEquals(fullBitmap0, fullBitmap1);
    }

    private RoaringBitmap getRandomRoaringBitmap() {
        final int[] data = RoaringBitmapTestUtils.takeSortedAndDistinct(
            new Random(0xcb000a2b9b5bdfb6L), MAX_BIT_NUM / 10, MAX_BIT_NUM
        );
        RoaringBitmap randomBitmap = RoaringBitmap.bitmapOf(data);
        // bitmap density and too many little runs
        for (int k = MAX_BIT_NUM / 10; k < MAX_BIT_NUM / 10 * 2; k++) {
            randomBitmap.add(3 * k);
        }
        // RunContainer would be best
        for (int k = MAX_BIT_NUM / 10 * 7; k < MAX_BIT_NUM / 10 * 8; k++) {
            randomBitmap.add(k);
        }
        // mix of all 3 container kinds
        randomBitmap.runOptimize();

        return randomBitmap;
    }
}
