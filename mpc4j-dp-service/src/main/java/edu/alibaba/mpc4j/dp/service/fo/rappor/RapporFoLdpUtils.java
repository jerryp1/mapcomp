package edu.alibaba.mpc4j.dp.service.fo.rappor;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;

import java.nio.ByteBuffer;

/**
 * RAPPOR Frequency Oracle LDP utilities.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class RapporFoLdpUtils {

    private RapporFoLdpUtils() {
        // empty
    }

    /**
     * Gets the size of the bloom filter.
     *
     * @param d the domain size.
     * @param hashNum the number of hashes.
     * @return the size of the bloom filter.
     */
    public static int getM(int d, int hashNum) {
        MathPreconditions.checkGreater("# of hashes", hashNum, 1);
        MathPreconditions.checkGreater("|Ω|", d, 1);
        // m = d · k / ln(2)
        return (int)Math.ceil(d * hashNum / Math.log(2));
    }

    /**
     * Calculates hash values for the given item. Given k hash seeds, this must return k distinct hash values in [0, m).
     *
     * @param intHash int hash function.
     * @param item the item.
     * @param m the hash value bound.
     * @param cohortHashSeeds the cohort hash seeds.
     * @return the hash values.
     */
    public static int[] hash(IntHash intHash, String item, int m, int[] cohortHashSeeds) {
        MathPreconditions.checkGreater("# of hashes", cohortHashSeeds.length, 1);
        int hashNum = cohortHashSeeds.length;
        MathPreconditions.checkGreaterOrEqual("m", m, hashNum);
        // it is OK to use a set to ensure distinct hash results. However, it is slow.
        // Since the number of hashes is relatively small, here we use the naive solution.
        byte[] itemBytes = item.getBytes(FoLdpFactory.DEFAULT_CHARSET);
        int[] hashValues = new int[hashNum];
        for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
            boolean distinct;
            int counter = 0;
            do {
                distinct = true;
                byte[] counterItemBytes = ByteBuffer.allocate(Integer.BYTES + itemBytes.length)
                    .putInt(counter)
                    .put(itemBytes)
                    .array();
                hashValues[hashIndex] = Math.abs(intHash.hash(counterItemBytes, cohortHashSeeds[hashIndex])) % m;
                counter++;
                // test if the hash value is distinct
                for (int previousHashIndex = 0; previousHashIndex < hashIndex; previousHashIndex++) {
                    if (hashValues[hashIndex] == hashValues[previousHashIndex]) {
                        distinct = false;
                        break;
                    }
                }
            } while (!distinct);
        }
        return hashValues;
    }
}
