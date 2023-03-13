package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2e;

import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Arrays;

/**
 * GF2E-VOLE receiver output. The receiver gets (Δ, q) with t = q + Δ·x, where x and t is owned by the sender.
 *
 * @author Weiran Liu
 * @date 2022/6/9
 */
public class Gf2eVoleReceiverOutput {
    /**
     * the GF2E instance
     */
    private Gf2e gf2e;
    /**
     * Δ
     */
    private byte[] delta;
    /**
     * q_i
     */
    private byte[][] q;

    /**
     * Creates a receiver output.
     *
     * @param gf2e  the GF2E instance.
     * @param delta Δ.
     * @param q     q_i.
     * @return the receiver output.
     */
    public static Gf2eVoleReceiverOutput create(Gf2e gf2e, byte[] delta, byte[][] q) {
        Gf2eVoleReceiverOutput receiverOutput = new Gf2eVoleReceiverOutput();
        receiverOutput.gf2e = gf2e;
        assert gf2e.validateElement(delta);
        receiverOutput.delta = BytesUtils.clone(delta);
        assert q.length > 0 : "# of q must be greater than 0";
        receiverOutput.q = Arrays.stream(q)
            .peek(qi -> {
                assert gf2e.validateElement(qi);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);

        return receiverOutput;
    }

    /**
     * Creates an empty receiver output.
     *
     * @param gf2e  the GF2E instance.
     * @param delta Δ.
     * @return an empty receiver output.
     */
    public static Gf2eVoleReceiverOutput createEmpty(Gf2e gf2e, byte[] delta) {
        Gf2eVoleReceiverOutput receiverOutput = new Gf2eVoleReceiverOutput();
        receiverOutput.gf2e = gf2e;
        assert gf2e.validateElement(delta);
        receiverOutput.delta = BytesUtils.clone(delta);
        receiverOutput.q = new byte[0][];

        return receiverOutput;
    }

    /**
     * 私有构造函数。
     */
    private Gf2eVoleReceiverOutput() {
        // empty
    }

    /**
     * Splits the receiver output with the split num.
     *
     * @param splitNum the split num.
     * @return a new receiver output with the split num.
     */
    public Gf2eVoleReceiverOutput split(int splitNum) {
        int num = getNum();
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]";
        // split q
        byte[][] subQ = new byte[splitNum][];
        byte[][] remainQ = new byte[num - splitNum][];
        System.arraycopy(q, 0, subQ, 0, splitNum);
        System.arraycopy(q, splitNum, remainQ, 0, num - splitNum);
        q = remainQ;

        return Gf2eVoleReceiverOutput.create(gf2e, delta, subQ);
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
            byte[][] remainQ = new byte[reduceNum][];
            System.arraycopy(q, 0, remainQ, 0, reduceNum);
            q = remainQ;
        }
    }

    /**
     * Merges two receiver outputs.
     *
     * @param that the other receiver output.
     */
    public void merge(Gf2eVoleReceiverOutput that) {
        assert this.gf2e.equals(that.gf2e) : "merged sender output must have the same GF2E instance ("
            + this.gf2e + " : " + that.gf2e + ")";
        // merged outputs must have the same Δ
        assert Arrays.equals(this.delta, that.delta) : "merged outputs must have the same Δ";
        byte[][] mergeQ = new byte[this.q.length + that.q.length][];
        System.arraycopy(this.q, 0, mergeQ, 0, this.q.length);
        System.arraycopy(that.q, 0, mergeQ, this.q.length, that.q.length);
        q = mergeQ;
    }

    /**
     * Gets Δ.
     *
     * @return Δ.
     */
    public byte[] getDelta() {
        return delta;
    }

    /**
     * Gets q_i.
     *
     * @param index the index.
     * @return q_i.
     */
    public byte[] getQ(int index) {
        return q[index];
    }

    /**
     * Gets q.
     *
     * @return q.
     */
    public byte[][] getQ() {
        return q;
    }

    /**
     * Gets num.
     *
     * @return num.
     */
    public int getNum() {
        return q.length;
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
