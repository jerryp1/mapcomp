package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Arrays;

/**
 * GF2E-VOLE sender output. The sender gets (x, t) with t = q + Δ·x, where Δ and q is owned by the receiver.
 *
 * @author Weiran Liu
 * @date 2022/6/9
 */
public class Gf2eVoleSenderOutput {
    /**
     * the GF2E instance
     */
    private Gf2e gf2e;
    /**
     * x_i
     */
    private byte[][] x;
    /**
     * t_i
     */
    private byte[][] t;

    /**
     * Creates a sender output.
     *
     * @param gf2e the GF2E instance.
     * @param x    x_i.
     * @param t    t_i.
     * @return a sender output.
     */
    public static Gf2eVoleSenderOutput create(Gf2e gf2e, byte[][] x, byte[][] t) {
        Gf2eVoleSenderOutput senderOutput = new Gf2eVoleSenderOutput();
        senderOutput.gf2e = gf2e;
        assert x.length > 0 : "# of x must be greater than 0";
        assert x.length == t.length : "# of x must be equal to # of q";
        senderOutput.x = Arrays.stream(x)
            .peek(xi -> {
                assert gf2e.validateElement(xi);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        senderOutput.t = Arrays.stream(t)
            .peek(ti -> {
                assert gf2e.validateElement(ti);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);

        return senderOutput;
    }

    /**
     * Creates an empty sender output.
     *
     * @param gf2e the GF2E instance.
     * @return an empty sender output.
     */
    public static Gf2eVoleSenderOutput createEmpty(Gf2e gf2e) {
        Gf2eVoleSenderOutput senderOutput = new Gf2eVoleSenderOutput();
        senderOutput.gf2e = gf2e;
        senderOutput.x = new byte[0][];
        senderOutput.t = new byte[0][];

        return senderOutput;
    }

    /**
     * private constructor.
     */
    private Gf2eVoleSenderOutput() {
        // empty
    }

    /**
     * Splits the sender output with the split num.
     *
     * @param splitNum the split num.
     * @return a new sender output with the split num.
     */
    public Gf2eVoleSenderOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]";
        // split x
        byte[][] subX = new byte[splitNum][];
        byte[][] remainX = new byte[num - splitNum][];
        System.arraycopy(x, 0, subX, 0, splitNum);
        System.arraycopy(x, splitNum, remainX, 0, num - splitNum);
        x = remainX;
        // split t
        byte[][] subT = new byte[splitNum][];
        byte[][] remainT = new byte[num - splitNum][];
        System.arraycopy(t, 0, subT, 0, splitNum);
        System.arraycopy(t, splitNum, remainT, 0, num - splitNum);
        t = remainT;

        return Gf2eVoleSenderOutput.create(gf2e, subX, subT);
    }

    /**
     * Reduces the sender output to the reduced num.
     *
     * @param reduceNum the reduced num.
     */
    public void reduce(int reduceNum) {
        int num = getNum();
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]";
        if (reduceNum < num) {
            // if the reduced num is less than num, do split. If not, keep the current state.
            byte[][] remainX = new byte[reduceNum][];
            System.arraycopy(x, 0, remainX, 0, reduceNum);
            x = remainX;
            byte[][] remainT = new byte[reduceNum][];
            System.arraycopy(t, 0, remainT, 0, reduceNum);
            t = remainT;
        }
    }

    /**
     * Merges two sender outputs.
     *
     * @param that the other sender output.
     */
    public void merge(Gf2eVoleSenderOutput that) {
        assert this.gf2e.equals(that.gf2e) : "merged sender output must have the same GF2E instance ("
            + this.gf2e + " : " + that.gf2e + ")";
        // merge x
        byte[][] mergeX = new byte[this.x.length + that.x.length][];
        System.arraycopy(this.x, 0, mergeX, 0, this.x.length);
        System.arraycopy(that.x, 0, mergeX, this.x.length, that.x.length);
        x = mergeX;
        // merge t
        byte[][] mergeT = new byte[this.t.length + that.t.length][];
        System.arraycopy(this.t, 0, mergeT, 0, this.t.length);
        System.arraycopy(that.t, 0, mergeT, this.t.length, that.t.length);
        t = mergeT;
    }

    /**
     * Gets x_i.
     *
     * @param index the index.
     * @return x_i.
     */
    public byte[] getX(int index) {
        return x[index];
    }

    /**
     * Gets x.
     *
     * @return x.
     */
    public byte[][] getX() {
        return x;
    }

    /**
     * Gets t_i.
     *
     * @param index the index.
     * @return t_i.
     */
    public byte[] getT(int index) {
        return t[index];
    }

    /**
     * Gets t.
     *
     * @return t.
     */
    public byte[][] getT() {
        return t;
    }

    /**
     * Gets num.
     *
     * @return num.
     */
    public int getNum() {
        return x.length;
    }

    /**
     * Gets the GF2E instance.
     *
     * @return the GF2E instance.
     */
    public Gf2e getGf2e() {
        return gf2e;
    }
}
