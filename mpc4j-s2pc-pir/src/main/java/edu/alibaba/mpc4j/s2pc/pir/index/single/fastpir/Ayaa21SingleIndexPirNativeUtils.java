package edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir;

import java.util.List;

/**
 * FastPIR native utils.
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class Ayaa21SingleIndexPirNativeUtils {

    private Ayaa21SingleIndexPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param modulusDegree poly modulus degree.
     * @param plainModulus  plain modulus.
     * @param coeffModulus  cipher modulus.
     * @return encryption params.
     */
    static native byte[] generateSealContext(int modulusDegree, long plainModulus, long[] coeffModulus);

    /**
     * generate key pair.
     *
     * @param encryptionParams SEAL encryption params.
     * @param steps            steps.
     * @return key pair.
     */
    static native List<byte[]> keyGen(byte[] encryptionParams, int[] steps);

    /**
     * NTT transformation.
     *
     * @param encryptionParams SEAL encryption params.
     * @param plaintextList    BFV plaintexts not in NTT form.
     * @return BFV plaintexts in NTT form.
     */
    static native List<byte[]> nttTransform(byte[] encryptionParams, long[][] plaintextList);

    /**
     * generate query.
     *
     * @param encryptionParams SEAL encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @param index            indices.
     * @param querySize        query size.
     * @return query ciphertexts.
     */
    static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey, int index,
                                             int querySize);

    /**
     * generate response.
     *
     * @param encryptionParams SEAL encryption params.
     * @param galoisKey        Galois keys.
     * @param query            query ciphertexts.
     * @param database         database.
     * @param elementColNum    element column size.
     * @return response ciphertexts。
     */
    static native byte[] generateResponse(byte[] encryptionParams, byte[] galoisKey, List<byte[]> query,
                                          byte[][] database, int elementColNum);

    /**
     * decode response.
     *
     * @param encryptionParams SEAL encryption params.
     * @param secretKey        secret key.
     * @param response         response ciphertext.
     * @return BFV plaintext.
     */
    static native long[] decodeResponse(byte[] encryptionParams, byte[] secretKey, byte[] response);
}
