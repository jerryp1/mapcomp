package edu.alibaba.mpc4j.crypto.fhe.utils;

import com.google.common.primitives.Longs;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import org.junit.Test;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author Qixian Zhou
 * @date 2023/7/17
 */
public class RandomSampleTest {




    @Test
    public void testSampleHammingWeightVector() {

        sampleHammingWeightVector(128, 64);
        sampleHammingWeightVector(127, 63);
        sampleHammingWeightVector(64, 64);
    }



    public void sampleHammingWeightVector(int length, int hammingWeight) {

        assert length >= hammingWeight;
        long[] result = RandomSample.sampleHammingWeightArray(length, hammingWeight);

        List<Long> longList = Longs.asList(result);
        int count = Collections.frequency(longList, 0L);

        assert length - hammingWeight == count;

    }






    @Test
    public void testSampleTriangle() {


        int nums = 10000;
        long[] results = RandomSample.sampleTriangle(nums);

        Map<Long, Integer> map = new HashMap<>();
        for(long c: results) {
            if (!map.containsKey(c)) {
                map.put(c, 1);
            }else {
                map.put(c, map.get(c) + 1);
            }
        }

        for (long key: map.keySet()) {
            System.out.println(String.format("value: %d, possibility: %.2f", key, (double) map.get(key) / nums));
        }



    }




    @Test
    public void testRandomUniform() {

        // test my implementation whether right
        long minVal = 0;
        long maxVal = 10;
        int num = 10000;
        long[] coeffs = RandomSample.samplePositiveUniform2(minVal, maxVal, num);

        Map<Long, Integer> map = new HashMap<>();
        for(long c: coeffs) {
            if (!map.containsKey(c)) {
                map.put(c, 1);
            }else {
                map.put(c, map.get(c) + 1);
            }
        }

        if (map.containsKey(maxVal)) {
            System.out.println("error! can not contain the right endpoint!");
        }
        for (int i = (int) minVal; i < maxVal; i++) {
            System.out.println(String.format("value: %d, possibility: %.2f", i, (double) map.get((long) i) / num));
        }
    }

}
