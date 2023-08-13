package edu.alibaba.mpc4j.crypto.fhe.utils;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.AttributeSetMethodGenerator;

import java.util.Random;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/7/14
 */
public class RandomSample {


    public static long[] sampleUniform(long minVal, long maxVal, int nums) {

//        assert maxVal > minVal;
//        Random random = new Random();

        return samplePositiveUniform(minVal, maxVal, nums);

//        return IntStream.range(0, nums)
//                .mapToLong(i -> minVal + (random.nextLong() % (maxVal - minVal)))
//                .toArray();

    }

    /**
     * Samples from a discrete triangle distribution.
     *
     * Samples num_samples values from [-1, 0, 1] with probabilities
     * [0.25, 0.5, 0.25], respectively.
     *
     * @param nums number of random values
     * @return a array of randomly samplede values
     */
    public static long[] sampleTriangle(int nums) {
        Random random = new Random();
        long[] results = new long[nums];
        for (int i = 0; i < nums; i++) {
            // uniformly random [0, 1, 2, 3]
            int r = random.nextInt(4);
            if (r == 0) {
                results[i] = -1;
            }else if (r == 1) {
                results[i] = 1;
            }else {
                results[i] = 0;
            }
        }
        return results;
    }


    public static long[] sampleHammingWeightArray(int length, int hammingWeight) {

        Random random = new Random();

        long[] results = new long[length];
        int totalWeight = 0;
        while (totalWeight < hammingWeight) {
            // [0, 1, ..., length - 1]
            int index = random.nextInt(length);
            if (results[index] == 0) {
                // uniformly sample from [0, 1]
                int r = random.nextInt(2);
                if (r == 0) {
                    results[index] = -1;
                }else {
                    results[index] = 1;
                }
                totalWeight += 1;
            }
        }
        return results;
    }
    public static long[] samplePositiveUniform(long minVal, long maxVal, int nums) {

        assert maxVal > minVal;
        Random random = new Random();

        return IntStream.range(0, nums)
                .mapToLong(i -> Math.abs(minVal + random.nextLong() % (maxVal - minVal)))
                .toArray();

    }

    public static long[] samplePositiveUniform2(long minVal, long maxVal, int nums) {

        assert maxVal > minVal;
        Random random = new Random();

        return IntStream.range(0, nums)
                .mapToLong(i -> (minVal + Math.abs(random.nextLong()) % (maxVal - minVal)))
                .toArray();
    }
    public static long sampleUniform(long minVal, long maxVal, Random random) {
        return minVal + Math.abs(random.nextLong()) % (maxVal - minVal);
    }



}
