package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt;

import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;

/**
 * SJ23 UCPSI params.
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
public class Sj23PeqtUcpsiParams {
    /**
     * bin num
     */
    public final int binNum;
    /**
     * max partition size per bin
     */
    public final int maxPartitionSizePerBin;
    /**
     * item encoded slot size
     */
    public final int itemEncodedSlotSize;
    /**
     * Paterson-Stockmeyer low degree
     */
    public final int psLowDegree;
    /**
     * query powers
     */
    public final int[] queryPowers;
    /**
     * plain modulus size
     */
    public final int plainModulusSize;
    /**
     * plain modulus
     */
    public final int plainModulus;
    /**
     * poly modulus degree
     */
    public final int polyModulusDegree;
    /**
     * item per ciphertext
     */
    public final int itemPerCiphertext;
    /**
     * ciphertext num
     */
    public final int ciphertextNum;
    /**
     * l bit length
     */
    public final int l;

    private Sj23PeqtUcpsiParams(int binNum, int maxPartitionSizePerBin, int itemEncodedSlotSize, int psLowDegree,
                                int[] queryPowers, int plainModulusSize, int plainModulus, int polyModulusDegree) {
        this.binNum = binNum;
        this.maxPartitionSizePerBin = maxPartitionSizePerBin;
        this.itemEncodedSlotSize = itemEncodedSlotSize;
        this.psLowDegree = psLowDegree;
        this.queryPowers = queryPowers;
        this.plainModulusSize = plainModulusSize;
        this.plainModulus = plainModulus;
        this.polyModulusDegree = polyModulusDegree;
        this.itemPerCiphertext = polyModulusDegree / itemEncodedSlotSize;
        this.ciphertextNum = binNum / itemPerCiphertext;
        this.l = itemEncodedSlotSize * plainModulusSize;
    }

    /**
     * serve log size 20, client log size 8.
     */
    public static final Sj23PeqtUcpsiParams SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_8 = new Sj23PeqtUcpsiParams(
        4096, 228, 2,
        4, new int[]{1, 2, 3, 4, 5, 10, 15, 35, 55, 75, 95, 115, 125, 130, 140},
        33, 123, 8192
    );

    /**
     * return encoded array.
     *
     * @param hashBinEntry hash bin entry.
     * @param shiftMask    shift mask.
     * @return encoded array.
     */
    public long[] getHashBinEntryEncodedArray(byte[] hashBinEntry, BigInteger shiftMask) {
        long[] encodedArray = new long[itemEncodedSlotSize];
        BigInteger input = BigIntegerUtils.byteArrayToNonNegBigInteger(hashBinEntry);
        for (int i = 0; i < itemEncodedSlotSize; i++) {
            encodedArray[i] = input.and(shiftMask).longValueExact();
            input = input.shiftRight(l);
        }
        return encodedArray;
    }
}
