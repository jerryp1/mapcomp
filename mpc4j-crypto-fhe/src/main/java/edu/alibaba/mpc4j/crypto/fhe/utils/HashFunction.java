package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.bouncycastle.crypto.digests.Blake2bDigest;

import java.util.Arrays;

/**
 * @author Qixian Zhou
 * @date 2023/8/30
 */
public class HashFunction {

    // 32-byte , 4 * 64-bit
    public final static int HASH_BLOCK_UINT64_COUNT = 4;

    public final static long[] HASH_ZERO_BLOCK;

    private final static Blake2bDigest BLAKE_2B;

    static {
        HASH_ZERO_BLOCK = new long[] {0, 0, 0, 0};
        BLAKE_2B = new Blake2bDigest(HASH_BLOCK_UINT64_COUNT * 64);
    }


    public static void hash(long[] input, int uint64Count, long[] destination) {

        // convert input to bytes
        byte[] inputBytes = LongUtils.longArrayToByteArray(input, (uint64Count * 64) / 8);


        // add data
        BLAKE_2B.update(inputBytes, 0, inputBytes.length);
        // hash
        byte[] hash = new byte[BLAKE_2B.getDigestSize()];
        BLAKE_2B.doFinal(hash, 0);
        // convert to long
        long[] temp = LongUtils.byteArrayToLongArray(hash);

        System.arraycopy(temp, 0, destination, 0, HASH_BLOCK_UINT64_COUNT);
    }



}
