package edu.alibaba.mpc4j.s2pc.pir.keyword.aaag22;

import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;

import java.util.Arrays;

/**
 * AAAG23 keyword PIR params.
 *
 * @author Liqiang Peng
 * @date 2023/6/16
 */
public class Aaag22KwPirParams implements KwPirParams {
    /**
     * plain modulus bit length
     */
    private final int plainModulusBitLength = 16;
    /**
     * poly modulus degree
     */
    private final int polyModulusDegree = 32768;
    /**
     * coeffs modulus bits
     */
    private final int[] coeffModulusBits = {60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60};
    /**
     * expect server size
     */
    private final int expectServerSize;
    /**
     * max retrieval size
     */
    private final int maxRetrievalSize;
    /**
     * column num
     */
    public int colNum;
    /**
     * row num
     */
    public int rowNum;
    /**
     * query ciphertext num
     */
    public int queryCiphertextNum;
    /**
     * database row num
     */
    public int databaseRowNum;
    /**
     * PIR object num
     */
    public int pirObjectNum;
    /**
     * PIR column num per object
     */
    public int pirColumnNumPerObj;
    /**
     * PIR database row num
     */
    public int pirDbRowNum;
    /**
     * keyword bit length
     */
    public int keywordPrfByteLength = 8;

    public byte[] encryptionParams;

    private Aaag22KwPirParams(int expectServerSize) {
        this.expectServerSize = expectServerSize;
        this.maxRetrievalSize = 1;
        this.encryptionParams = Aaag22KwPirNativeUtils.genEncryptionParameters(
            polyModulusDegree, (1 << plainModulusBitLength)+ 1, coeffModulusBits
        );
    }

    public static Aaag22KwPirParams DEFAULT_PARAMS = new Aaag22KwPirParams(1000000);

    /**
     * initialize PIR params.
     *
     * @param num            database size.
     * @param labelBitLength label bit length.
     */
    public void initPirParams(int num, int labelBitLength) {
        this.colNum = CommonUtils.getUnitNum(keywordPrfByteLength * Byte.SIZE , 2 * plainModulusBitLength);
        this.rowNum = CommonUtils.getUnitNum(num, polyModulusDegree / 2);
        this.pirObjectNum = rowNum * (polyModulusDegree / 2);
        this.queryCiphertextNum = rowNum;
        if (CommonUtils.getUnitNum(labelBitLength, plainModulusBitLength) % 2 == 1) {
            labelBitLength = plainModulusBitLength + labelBitLength;
        }
        this.pirColumnNumPerObj = 2 * CommonUtils.getUnitNum((labelBitLength / 2), plainModulusBitLength);
        this.pirDbRowNum = CommonUtils.getUnitNum(pirObjectNum, polyModulusDegree) * pirColumnNumPerObj;
    }

    /**
     * return plain modulus bit length.
     *
     * @return plain modulus bit length.
     */
    public int getPlainModulusSize() {
        return plainModulusBitLength;
    }

    /**
     * return poly modulus degree.
     *
     * @return poly modulus degree.
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * return coeffs modulus bit.
     *
     * @return coeffs modulus bit.
     */
    public int[] getCoeffModulusBits() {
        return coeffModulusBits;
    }

    @Override
    public int maxRetrievalSize() {
        return maxRetrievalSize;
    }

    @Override
    public int expectServerSize() {
        return expectServerSize;
    }

    @Override
    public String toString() {
        return
            " Encryption parameters: {" + "\n" +
            "     - plain_modulus_size : " + plainModulusBitLength + "\n" +
            "     - poly_modulus_degree : " + polyModulusDegree + "\n" +
            "     - coeff_modulus_bits : " + Arrays.toString(coeffModulusBits) + "\n" +
            "  }" + "\n";
    }
}