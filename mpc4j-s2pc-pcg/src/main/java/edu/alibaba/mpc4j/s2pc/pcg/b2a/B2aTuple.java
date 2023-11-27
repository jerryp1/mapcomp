package edu.alibaba.mpc4j.s2pc.pcg.b2a;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

/**
 * Z2 triple.
 *
 * @author Li Peng
 * @date 2023/11/15
 */
public class B2aTuple implements MergedPcgPartyOutput {
    /**
     * triple num
     */
    private int num;
    /**
     * 'a'
     */
    private BitVector a;
    /**
     * 'b'
     */
    private ZlVector b;

    public static B2aTuple create(BitVector a, ZlVector b) {
        assert a.bitNum() == b.getNum() : "num of a must match b";
        B2aTuple b2ATuple = new B2aTuple();
        b2ATuple.a = a;
        b2ATuple.b = b;
        b2ATuple.num = a.bitNum();
        return b2ATuple;
    }

    public BitVector getA() {
        return a;
    }

    public ZlVector getB() {
        return b;
    }

    /**
     * private constructor.
     */
    private B2aTuple() {
        // empty
    }

    @Override
    public int getNum() {
        return num;
    }

    @Override
    public B2aTuple split(int splitNum) {
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        BitVector splitA = a.split(splitNum);
        ZlVector splitB = b.split(splitNum);
        num = num - splitNum;

        return B2aTuple.create(splitA, splitB);
    }

    @Override
    public void reduce(int reduceNum) {
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
        if (reduceNum < num) {
            // if the reduced num is less than num, do split. If not, keep the current state.
            a.reduce(reduceNum);
            b.reduce(reduceNum);
            // update num
            num = reduceNum;
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        B2aTuple that = (B2aTuple) other;
        a.merge(that.a);
        b.merge(that.b);
        // update num
        num += that.num;
    }


    @Override
    public String toString() {
        return "[" + a.toString() + ", " + b.toString() + "] (n = " + num + ")";
    }
}
