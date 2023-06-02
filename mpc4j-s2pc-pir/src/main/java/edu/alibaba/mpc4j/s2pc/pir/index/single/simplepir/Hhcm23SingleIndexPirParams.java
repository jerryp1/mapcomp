package edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

/**
 * Simple PIR scheme params with 128-bit security.
 *
 * @author Liqiang Peng
 * @date 2023/5/26
 */
public class Hhcm23SingleIndexPirParams implements SingleIndexPirParams {
    /**
     * the integer modulus
     */
    public long q;
    /**
     * the plaintext modulus
     */
    public int p;
    /**
     * the lwe secret length
     */
    public int n;
    /**
     * the standard deviation for sampling random elements
     */
    public double stdDev;
    /**
     * zl64
     */
    public Zl64 zl64;
    /**
     * expect element log size
     */
    public int expectElementLogSize;

    public Hhcm23SingleIndexPirParams(int n, int p, int l, double stdDev, int expectElementLogSize) {
        this.n = n;
        this.p = p;
        this.q = 1L << l;
        this.stdDev = stdDev;
        this.zl64 = Zl64Factory.createInstance(EnvType.STANDARD_JDK, l);
        this.expectElementLogSize = expectElementLogSize;
    }

    /**
     * server element log size 30
     */
    public static Hhcm23SingleIndexPirParams SERVER_ELEMENT_LOG_SIZE_30 = new Hhcm23SingleIndexPirParams(
        1024, 701, 32, 6.4, 30
    );

    @Override
    public int getPlainModulusBitLength() {
        return 0;
    }

    @Override
    public int getPolyModulusDegree() {
        return 0;
    }

    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public byte[] getEncryptionParams() {
        return null;
    }

    @Override
    public String toString() {
        return
            "LWE encryption parameters : " + "\n" +
                " - n : " + n + "\n" +
                " - q : " + q + "\n" +
                " - std-dev : " + stdDev;
    }
}
