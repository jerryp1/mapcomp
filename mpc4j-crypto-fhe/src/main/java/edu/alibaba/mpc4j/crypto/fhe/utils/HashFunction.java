package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.bouncycastle.crypto.digests.Blake2bDigest;

/**
 * HashFunction used to calculate the id of the EncryptionParams object.
 * The implementation is from:
 * <p>
 * https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/hash.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/30
 */
public class HashFunction {
    /**
     * 32-byte, 4 * 64-bit
     */
    public final static int HASH_BLOCK_UINT64_COUNT = 4;
    /**
     * hash of zero block
     */
    public final static long[] HASH_ZERO_BLOCK = new long[]{0, 0, 0, 0};
    /**
     * Black2B hash
     */
    private final static Blake2bDigest BLAKE_2B = new Blake2bDigest(HASH_BLOCK_UINT64_COUNT * 64);

    /**
     * Computes the hash of the input.
     *
     * @param input       the input data.
     * @param uint64Count length of the data in uint64 size.
     * @param destination place to set the output.
     */
    public static void hash(long[] input, int uint64Count, long[] destination) {
        // convert input to bytes and hash
        byte[] inputBytes = LongUtils.longArrayToByteArray(input, (uint64Count * 64) / 8);
        BLAKE_2B.update(inputBytes, 0, inputBytes.length);
        byte[] hash = new byte[BLAKE_2B.getDigestSize()];
        BLAKE_2B.doFinal(hash, 0);
        // convert back to long
        long[] temp = LongUtils.byteArrayToLongArray(hash);
        System.arraycopy(temp, 0, destination, 0, HASH_BLOCK_UINT64_COUNT);
    }
}
