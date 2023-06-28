package edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * provide constant-weight code and folklore code implementation
 *
 * @author Qixian Zhou
 * @date 2023/6/19
 */
public class Mk22SingleIndexPirUtils {

    /**
     * Algorithm 3 Perfect mapping
     *
     * @param input             input
     * @param codewordBitLength m
     * @param hammingWeight     k
     * @return
     */
    public static int[] getPerfectConstantWeightCodeword(int input, int codewordBitLength, int hammingWeight) {

        int n = (int) DoubleUtils.estimateCombinatorial(codewordBitLength, hammingWeight);
        assert input < n : "input must be smaller than size of the codewords";
        int[] codeword = new int[codewordBitLength];

        int remainder = input;
        int kPrime = hammingWeight;
        for (int p = codewordBitLength - 1; p >= 0; p--) {
            int temp;
            if (kPrime > p) {
                temp = 0;
            } else {
                temp = (int) DoubleUtils.estimateCombinatorial(p, kPrime);
            }
            if (remainder >= temp) {
                codeword[p] = 1;
                remainder -= temp;
                kPrime -= 1;
            }
        }
        return codeword;
    }

    public static int[] getFolkloreCodeword(int input, int codewordBitLength) {

        int n = 1 << codewordBitLength;
        assert input < n : "input must be smaller than size of the codewords";
        int[] codeword = new int[codewordBitLength];
        int remainder = input;
        for (int p = 0; p < codewordBitLength; p++) {
            codeword[p] = remainder % 2;
            remainder /= 2;
        }
        return codeword;
    }


    /**
     * Given k, n; find m satisfying C_m^k >= n
     *
     * @param hammingWeight k
     * @param codewordSize  n
     * @return
     */
    public static int getCodewordBitLength(int hammingWeight, int codewordSize) {

        int codewordBitLength = hammingWeight;
        while ((int) DoubleUtils.estimateCombinatorial(codewordBitLength, hammingWeight) < codewordSize) {
            codewordBitLength += 1;
        }
        return codewordBitLength;
    }

    /**
     * generate the codeword of index in range [0, plaintextSize -1 ]
     *
     * @param plaintextSize
     * @param equalityType
     * @return
     */
    public static List<int[]> getPlaintextIndexCodeword(int plaintextSize, int codewordBitLength, int hammingWeight, Mk22SingleIndexPirParams.EqualityType equalityType) {

        assert plaintextSize > 0;
        List<int[]> codewords = new ArrayList<>();
        switch (equalityType) {
            case FOLKLORE:
                for (int i = 0; i < plaintextSize; i++) {
                    codewords.add(getFolkloreCodeword(i, codewordBitLength));
                }
                break;
            case CONSTANT_WEIGHT:
                for (int i = 0; i < plaintextSize; i++) {
                    codewords.add(getPerfectConstantWeightCodeword(i, codewordBitLength, hammingWeight));
                }
                break;
            default:
                throw new IllegalStateException("Invalid Equality Operator Type");
        }
        return codewords;
    }

}
