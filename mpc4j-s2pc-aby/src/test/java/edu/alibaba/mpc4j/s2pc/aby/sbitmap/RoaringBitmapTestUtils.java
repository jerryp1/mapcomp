package edu.alibaba.mpc4j.s2pc.aby.sbitmap;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Random;

/**
 * Utilities for RoaringBitmap tests. Some source codes are from:
 * <p>
 * https://github.com/RoaringBitmap/RoaringBitmap/tree/master/RoaringBitmap/src/test
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/12/28
 */
public class RoaringBitmapTestUtils {
    /**
     * private constructor.
     */
    private RoaringBitmapTestUtils() {
        // empty
    }

    /**
     * Generate a sorted array containing {@code count} integers.
     *
     * @param random the random state.
     * @param count  the number of integers in the array.
     * @param max    the maximal integer value (excluded).
     * @return sorted array containing {@code count} integers.
     */
    public static int[] takeSortedAndDistinct(Random random, int count, int max) {
        return takeSortedAndDistinct(random, count, 0, max);
    }

    /**
     * Generate a sorted array containing {@code count} integers.
     *
     * @param random the random state.
     * @param count  the number of integers in the array.
     * @param min    the minimal integer value (included).
     * @param max    the maximal integer value (excluded).
     * @return sorted array containing {@code count} integers.
     */
    public static int[] takeSortedAndDistinct(Random random, int count, int min, int max) {
        MathPreconditions.checkNonNegative("min", min);
        MathPreconditions.checkGreater("max", max, min);
        // we require count < max / 2 so that we can quickly generate the int array
        int range = max - min;
        MathPreconditions.checkNonNegativeInRange("count", count, range / 2);
        LinkedHashSet<Integer> ints = new LinkedHashSet<>(count);
        for (int size = 0; size < count; size++) {
            int next;
            do {
                next = Math.abs(random.nextInt(range) + min);
            } while (!ints.add(next));
        }
        int[] unboxed = ints.stream().mapToInt(value -> value).toArray();
        Arrays.sort(unboxed);
        return unboxed;
    }
}
