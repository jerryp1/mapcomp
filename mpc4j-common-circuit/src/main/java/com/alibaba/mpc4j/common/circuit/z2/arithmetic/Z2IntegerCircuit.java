package com.alibaba.mpc4j.common.circuit.z2.arithmetic;

import com.alibaba.mpc4j.common.circuit.z2.MpcBcParty;
import com.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import com.alibaba.mpc4j.common.circuit.z2.MpcZ2VectorFactory;
import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Arrays;

/**
 * Z2 Integer Circuit.
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/20
 */
public class Z2IntegerCircuit extends AbstractZ2ArithmeticCircuit implements Z2ArithmeticCircuit {

    public Z2IntegerCircuit(MpcBcParty party) {
        super(party);
    }

    @Override
    public MpcZ2Vector[] add(MpcZ2Vector[] x, MpcZ2Vector[] y) throws MpcAbortException {
        return add(x, y, false);
    }

    public MpcZ2Vector[] add(MpcZ2Vector[] x, MpcZ2Vector[] y, boolean cin) throws MpcAbortException {
        checkInputs(x, y);

        return Arrays.copyOfRange(addFull(x, y, cin), 1, x.length + 1);
    }

    /**
     * Full 1-bit adders.
     *
     * @param x   input x.
     * @param y   input y.
     * @param cin MpcZ2Vector carry in.
     * @return result of x + y with carry out bit.
     */
    protected MpcZ2Vector[] add(MpcZ2Vector x, MpcZ2Vector y, MpcZ2Vector cin) throws MpcAbortException {
        MpcZ2Vector[] res = new MpcZ2Vector[2];

        MpcZ2Vector t1 = party.xor(x, cin);
        MpcZ2Vector t2 = party.xor(y, cin);
        res[0] = party.xor(x, t2);
        t1 = party.and(t1, t2);
        res[1] = party.xor(cin, t1);
        return res;
    }

    /**
     * Full n-bit adders. Computation is performed in big-endian order.
     *
     * @param x   input x.
     * @param y   input y.
     * @param cin boolean carry in.
     * @return result of x + y with carry out bit.
     */
    public MpcZ2Vector[] addFull(MpcZ2Vector[] x, MpcZ2Vector[] y, boolean cin) throws MpcAbortException {
        checkInputs(x, y);
        int bitLen = x[0].getNum();

        MpcZ2Vector cinVector = MpcZ2VectorFactory.create(x[0].getType(), bitLen, cin);
        return addFull(x, y, cinVector);
    }

    /**
     * full n-bit adders. Computation is performed in big-endian order.
     *
     * @param x   input x.
     * @param y   input y.
     * @param cin MpcZ2Vector carry in.
     * @return result of x + y with carry out bit.
     */
    public MpcZ2Vector[] addFull(MpcZ2Vector[] x, MpcZ2Vector[] y, MpcZ2Vector cin) throws MpcAbortException {
        checkInputs(x, y);

        MpcZ2Vector[] res = new MpcZ2Vector[x.length + 1];
        MpcZ2Vector[] t = add(x[x.length - 1], y[y.length - 1], cin);
        res[res.length - 1] = t[0];
        for (int i = res.length - 1; i > 1; i--) {
            t = add(x[i - 2], y[i - 2], t[1]);
            res[i - 1] = t[0];
        }
        res[0] = t[1];
        return res;
    }

    @Override
    public MpcZ2Vector[] sub(MpcZ2Vector[] x, MpcZ2Vector[] y) throws MpcAbortException {
        checkInputs(x, y);
        return add(x, not(y), true);
    }

    @Override
    public MpcZ2Vector leq(MpcZ2Vector[] x, MpcZ2Vector[] y) throws MpcAbortException {
        checkInputs(x, y);
        MpcZ2Vector[] result = sub(x, y);
        return result[0];
    }

    @Override
    public MpcZ2Vector[] bitAdd(MpcZ2Vector x, MpcZ2Vector y, MpcZ2Vector cin) throws MpcAbortException {
        return add(x, y, cin);
    }


    private void checkInputs(MpcZ2Vector[] x, MpcZ2Vector[] y) {
        Preconditions.checkArgument(x != null && y != null, "Null input data");
        Preconditions.checkArgument(x.length == y.length,
                "Array length of x and y not match");
    }

    public MpcZ2Vector[] not(MpcZ2Vector[] x) throws MpcAbortException {
        assert x != null;
        MpcZ2Vector[] result = new MpcZ2Vector[x.length];
        for (int i = 0; i < x.length; ++i) {
            result[i] = not(x[i]);
        }
        return result;
    }

    public MpcZ2Vector not(MpcZ2Vector x) throws MpcAbortException {
        assert x != null;

        MpcZ2Vector one = MpcZ2VectorFactory.createOnes(x.getType(), x.getNum());
        return party.xor(x, one);
    }


}
