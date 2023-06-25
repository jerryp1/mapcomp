package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.util.List;

/**
 * AAAG22 keyword native utils.
 *
 * @author Liqiang Peng
 * @date 2023/6/16
 */
class Aaag22KwPirNativeUtils {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    private Aaag22KwPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulusSize  plain modulus size.
     * @param coeffModulusBits  coeffs modulus bits.
     * @return encryption params.
     */
    static native byte[] genEncryptionParameters(int polyModulusDegree, long plainModulusSize, int[] coeffModulusBits);

    /**
     * generate key pair.
     *
     * @param encryptionParams encryption params.
     * @return key pair.
     */
    static native List<byte[]> keyGen(byte[] encryptionParams, int pirColumnNumPerObj, int colNum);

    static native List<byte[]> nttTransform(byte[] encryptionParams, long[][] coeffs);

    static native List<byte[]> preprocessMask(byte[] encryptionParams, int colNum);

    /**
     * generate query.
     *
     * @param encryptionParams encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @param query            query.
     * @return client query.
     */
    static native byte[] generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey,
                                             long[] query);

    static native List<byte[]> expandQuery(byte[] encryptionParams, byte[] galoisKeys, List<byte[]> masks, byte[] query, int colNum);

    static native byte[] processColumn(byte[] encryptionParams, byte[] publicKey, byte[] relinKeys, long[] pt, byte[] ct);

    static native byte[] processRow(byte[] encryptionParams, byte[] relinKeys, byte[] galoisKeys, List<byte[]> columnResults);

    static native byte[] processPir(byte[] encryptionParams, byte[] galoisKeys, List<byte[]> encodedLabel, List<byte[]>
                                    rowResults, int queryCiphertextNum, int columnNumPerObj);

    /**
     * decode server response.
     *
     * @param encryptedResponse server response.
     * @param encryptionParams  encryption params.
     * @param secretKey         secret key.
     * @return retrieval result.
     */
    static native long[] decodeReply(byte[] encryptionParams, byte[] secretKey, byte[] encryptedResponse);
}
