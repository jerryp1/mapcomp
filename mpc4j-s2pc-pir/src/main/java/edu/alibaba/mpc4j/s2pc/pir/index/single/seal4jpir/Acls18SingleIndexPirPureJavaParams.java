package edu.alibaba.mpc4j.s2pc.pir.index.single.seal4jpir;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.params.EncryptionParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;


/**
 * @author Qixian Zhou
 * @date 2023/10/13
 */
public class Acls18SingleIndexPirPureJavaParams implements SingleIndexPirParams {


    /**
     * plain modulus size
     */
    private final int plainModulusBitLength;
    /**
     * poly modulus degree
     */
    private final int polyModulusDegree;
    /**
     * dimension
     */
    private final int dimension;
    /**
     * SEAL encryption params
     */
    private final EncryptionParams encryptionParams;
    /**
     * expansion ratio
     */
    private final int expansionRatio;

    private final Context context;

    public Acls18SingleIndexPirPureJavaParams(int polyModulusDegree, int plainModulusBitLength, int dimension) {
        this.polyModulusDegree = polyModulusDegree;
        this.plainModulusBitLength = plainModulusBitLength;
        this.dimension = dimension;

        this.encryptionParams = Acls18SingleIndexPirPureJavaUtils.generateEncryptionParams(
                polyModulusDegree, (1L << plainModulusBitLength) + 1
        );

        this.context = new Context(this.encryptionParams);

        this.expansionRatio = Acls18SingleIndexPirPureJavaUtils.expansionRatio(this.context);
    }

    /**
     * default params
     */
    public static Acls18SingleIndexPirPureJavaParams DEFAULT_PARAMS =
            new Acls18SingleIndexPirPureJavaParams(
                    4096,
                    20,
                    2);

    @Override
    public int getPlainModulusBitLength() {
        return plainModulusBitLength;
    }

    @Override
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    /**
     * encryption params.
     *
     * @return encryption params.
     */
    @Override
    public byte[] getEncryptionParams() {
        return new byte[0];
    }


    public EncryptionParams getEncryptionParamsSelf() {
        return encryptionParams;
    }

    public Context getContext() {
        return context;
    }

    /**
     * return expansion ratio.
     *
     * @return expansion ratio.
     */
    public int getExpansionRatio() {
        return expansionRatio;
    }

    @Override
    public String toString() {
        return
                "SEAL encryption parameters : " + "\n" +
                        " - degree of polynomial modulus : " + polyModulusDegree + "\n" +
                        " - size of plaintext modulus : " + plainModulusBitLength + "\n" +
                        " - dimension : " + dimension;
    }
}