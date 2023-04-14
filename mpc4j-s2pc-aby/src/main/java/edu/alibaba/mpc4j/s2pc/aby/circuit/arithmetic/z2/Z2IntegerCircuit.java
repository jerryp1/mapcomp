package edu.alibaba.mpc4j.s2pc.aby.circuit.arithmetic.z2;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

import java.util.Arrays;

/**
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/12
 */
public class Z2IntegerCircuit extends AbstractZ2ArithmeticCircuit implements Z2ArithmeticCircuit {

    static final int S = 0;
    static final int COUT = 1;

    public Z2IntegerCircuit(BcParty e) {
        super(e);
    }

    @Override
    public SquareShareZ2Vector[] add(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) {
        return null;
    }

    public SquareShareZ2Vector[] add(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y, boolean cin) throws MpcAbortException {
        return Arrays.copyOf(addFull(x, y, cin), x.length);
    }

    // full 1-bit adder
    protected SquareShareZ2Vector[] add(SquareShareZ2Vector x, SquareShareZ2Vector y, SquareShareZ2Vector cin) throws MpcAbortException {
        SquareShareZ2Vector[] res = new SquareShareZ2Vector[2];

        SquareShareZ2Vector t1 = env.xor(x, cin);
        SquareShareZ2Vector t2 = env.xor(y, cin);
        res[S] = env.xor(x, t2);
        t1 = env.and(t1, t2);
        res[COUT] = env.xor(cin, t1);
        return res;
    }

    // full n-bit adder
    public SquareShareZ2Vector[] addFull(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y, boolean cin) throws MpcAbortException {
        assert (x != null && y != null && x.length == y.length) : "add: bad inputs.";

        SquareShareZ2Vector[] res = new SquareShareZ2Vector[x.length + 1];
        // 这里假设刚好是length长度
        byte[] cinBytes = cin ? new byte[x[0].getBytes().length] : BytesUtils.not(new byte[x[0].getBytes().length], x[0].getNum());

        SquareShareZ2Vector cinVector = SquareShareZ2Vector.create(
                BitVectorFactory.create(x[0].getNum(), cinBytes), false);
        SquareShareZ2Vector[] t = add(x[0], y[0], cinVector);
        res[0] = t[S];
        for (int i = 0; i < x.length - 1; i++) {
            t = add(x[i + 1], y[i + 1], t[COUT]);
            res[i + 1] = t[S];
        }
        res[res.length - 1] = t[COUT];
        return res;
    }

    @Override
    public SquareShareZ2Vector[] sub(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) throws MpcAbortException {
        checkInputs(x, y);

        return add(x, not(y), true);
    }

    // 以下的所有逻辑能否写到接口的默认实现方法中
    @Override
    public SquareShareZ2Vector leq(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) throws MpcAbortException {
        checkInputs(x, y);
        SquareShareZ2Vector[] result = sub(x, y);
        return result[result.length - 1];
    }

    @Override
    public SquareShareZ2Vector[] bitAdd(SquareShareZ2Vector x, SquareShareZ2Vector y, SquareShareZ2Vector cin) throws MpcAbortException {
        return add(x, y, cin);
    }


    private void checkInputs(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) {
        Preconditions.checkArgument(x != null && y != null, "Null input data");
        Preconditions.checkArgument(x.length ==   y.length,
                "Array length of x and y not match");
    }

    public SquareShareZ2Vector[] not(SquareShareZ2Vector[] x) throws MpcAbortException {
        assert x!= null;
        SquareShareZ2Vector[] result = new SquareShareZ2Vector[x.length];
        for (int i = 0; i < x.length; ++i) {
            result[i] = not(x[i]);
        }
        return result;
    }

    public SquareShareZ2Vector not(SquareShareZ2Vector x) throws MpcAbortException {
        assert x!= null;

        SquareShareZ2Vector one = SquareShareZ2Vector.createOnes(x.getNum());
//        SquareShareZ2Vector result =  env.xor(x, one);
        return env.xor(x, one);
    }

}
