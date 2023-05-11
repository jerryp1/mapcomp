package edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir;

import java.util.List;

/**
 * Vectorized PIR native utils.
 *
 * @author Liqiang Peng
 * @date 2023/3/6
 */
class Mr23SingleIndexPirNativeUtils {

    private Mr23SingleIndexPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param modulusDegree         poly modulus degree.
     * @param plainModulusBitLength plain modulus.
     * @return encryption params.
     */
    static native byte[] generateSealContext(int modulusDegree, int plainModulusBitLength);

    /**
     * generate key pair.
     *
     * @param encryptionParams SEAL encryption params.
     * @param slotNum          slot num.
     * @return key pair.
     */
    static native List<byte[]> keyGen(byte[] encryptionParams, int slotNum);

    /**
     * database preprocess.
     *
     * @param encryptionParams SEAL encryption params.
     * @param db               database.
     * @param dimensionsSize   dimension size.
     * @param plaintextSize    plaintext size.
     * @return BFV plaintexts in NTT form.
     */
    static native List<byte[]> preprocessDatabase(byte[] encryptionParams, long[] db, int[] dimensionsSize,
                                                  int plaintextSize);

    /**
     * generate query.
     *
     * @param encryptionParams SEAL encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @param indices          indices.
     * @param slotNum          slot num.
     * @return query ciphertexts.
     */
    static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey, int[] indices,
                                             int slotNum);

    /**
     * generate response.
     *
     * @param encryptionParams      SEAL encryption params.
     * @param queryList             query ciphertext.
     * @param dbPlaintexts          encoded database.
     * @param publicKey             public key.
     * @param relinKeys             relinearization keys.
     * @param galoisKeys            Galois keys.
     * @param firstTwoDimensionSize first two dimension size.
     * @param thirdDimensionSize    third dimension size.
     * @return response ciphertexts。
     */
    static native byte[] generateReply(byte[] encryptionParams, List<byte[]> queryList, byte[][] dbPlaintexts,
                                       byte[] publicKey, byte[] relinKeys, byte[] galoisKeys, int firstTwoDimensionSize,
                                       int thirdDimensionSize);

    /**
     * decode response.
     *
     * @param encryptionParams SEAL encryption params.
     * @param secretKey        secret key.
     * @param response         response ciphertext.
     * @param offset           offset.
     * @param slotNum          slot num.
     * @return coefficient.
     */
    static native long decryptReply(byte[] encryptionParams, byte[] secretKey, byte[] response, int offset, int slotNum);
}
