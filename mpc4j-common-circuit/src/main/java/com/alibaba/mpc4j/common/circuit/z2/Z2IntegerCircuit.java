package com.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Z2 Integer Circuit.
 *
 * @author Li Peng
 * @date 2023/4/20
 */
public class Z2IntegerCircuit {
    /**
     * MPC boolean circuit party.
     */
    private final MpcBcParty party;

    public Z2IntegerCircuit(MpcBcParty party) {
        this.party = party;
    }

    /**
     * addition.
     *
     * @param xs x array.
     * @param ys y array.
     * @return z = x + y, where z = x + y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector[] add(MpcZ2Vector[] xs, MpcZ2Vector[] ys) throws MpcAbortException {
        checkInputs(xs, ys);
        return add(xs, ys, false);
    }

    /**
     * increase 1.
     *
     * @param xs x array.
     * @return x + 1.
     */
    public MpcZ2Vector[] increaseOne(MpcZ2Vector[] xs) throws MpcAbortException {
        checkInputs(xs);
        int l = xs.length;
        int bitNum = xs[0].getNum();
        MpcZ2Vector[] ys = IntStream.range(0, l).mapToObj(i -> party.createZeros(bitNum)).toArray(MpcZ2Vector[]::new);
        return add(xs, ys, true);
    }

    private MpcZ2Vector[] add(MpcZ2Vector[] xs, MpcZ2Vector[] ys, boolean cin) throws MpcAbortException {
        int bitNum = xs[0].getNum();
        MpcZ2Vector cinVector = party.create(bitNum, cin);
        MpcZ2Vector[] zs = addFullBits(xs, ys, cinVector);
        // ignore the highest carry_out bit.
        return Arrays.copyOfRange(zs, 1, xs.length + 1);
    }

    /**
     * Full n-bit adders. Computation is performed in big-endian order.
     *
     * @param xs  x array in big-endian order.
     * @param ys  y array in big-endian order.
     * @param cin carry_in bit.
     * @return (carry_out bit, result).
     */
    private MpcZ2Vector[] addFullBits(MpcZ2Vector[] xs, MpcZ2Vector[] ys, MpcZ2Vector cin) throws MpcAbortException {
        MpcZ2Vector[] zs = new MpcZ2Vector[xs.length + 1];
        MpcZ2Vector[] t = addOneBit(xs[xs.length - 1], ys[ys.length - 1], cin);
        zs[zs.length - 1] = t[0];
        for (int i = zs.length - 1; i > 1; i--) {
            t = addOneBit(xs[i - 2], ys[i - 2], t[1]);
            zs[i - 1] = t[0];
        }
        zs[0] = t[1];
        return zs;
    }

    /**
     * Full 1-bit adders.
     *
     * @param x x.
     * @param y y.
     * @param c carry-in bit.
     * @return (carry_out bit, result).
     */
    private MpcZ2Vector[] addOneBit(MpcZ2Vector x, MpcZ2Vector y, MpcZ2Vector c) throws MpcAbortException {
        MpcZ2Vector[] z = new MpcZ2Vector[2];
        MpcZ2Vector t1 = party.xor(x, c);
        MpcZ2Vector t2 = party.xor(y, c);
        z[0] = party.xor(x, t2);
        t1 = party.and(t1, t2);
        z[1] = party.xor(c, t1);
        return z;
    }

    private void checkInputs(MpcZ2Vector[] xs, MpcZ2Vector[] ys) {
        int l = xs.length;
        MathPreconditions.checkPositive("l", l);
        // check equal l.
        MathPreconditions.checkEqual("l", "y.length", l, ys.length);
        // check equal num for all vectors.
        int num = xs[0].getNum();
        IntStream.range(0, l).forEach(i -> {
            MathPreconditions.checkEqual("num", "xi.num", num, xs[i].getNum());
            MathPreconditions.checkEqual("num", "yi.num", num, ys[i].getNum());
        });
    }

    private void checkInputs(MpcZ2Vector[] xs) {
        int l = xs.length;
        MathPreconditions.checkPositive("l", l);
        // check equal num for all vectors.
        int num = xs[0].getNum();
        IntStream.range(0, l).forEach(i -> MathPreconditions.checkEqual("num", "xi.num", num, xs[i].getNum()));
    }
}
