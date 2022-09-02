package edu.alibaba.mpc4j.common.tool.crypto.kyber.kyber4j;

/**
 * Helper class for unpacked cipher text
 *
 * @author Steven K Fisher <swiftcryptollc@gmail.com>
 */
public class UnpackedCipherText {
    private short[][] u;
    private short[] v;

    /**
     * Default Constructor
     */
    public UnpackedCipherText() {

    }

    /**
     * @return the u
     */
    public short[][] getU() {
        return u;
    }

    /**
     * @param u the bp to set
     */
    public void setU(short[][] u) {
        this.u = u;
    }

    /**
     * @return the v
     */
    public short[] getV() {
        return v;
    }

    /**
     * @param v the v to set
     */
    public void setV(short[] v) {
        this.v = v;
    }
}
