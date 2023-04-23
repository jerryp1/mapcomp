package edu.alibaba.mpc4j.s2pc.aby.circuit.z2;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

import java.util.Arrays;

/**
 * Z2 Integer Circuit.
 * // 名字改成party
 *
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2023/4/12
 */
public class Z2IntegerCircuit extends AbstractZ2ArithmeticCircuit implements Z2ArithmeticCircuit {

    static final int S = 0;
    static final int COUT = 1;

    // 传入sender/receiver
    public Z2IntegerCircuit(BcParty e) {
        super(e);
    }

    @Override
    public SquareShareZ2Vector[] add(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) throws MpcAbortException {
        return add(x, y, false);
    }

    public SquareShareZ2Vector[] add(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y, boolean cin) throws MpcAbortException {
        checkInputs(x, y);

        return Arrays.copyOfRange(addFull(x, y, cin), 1, x.length + 1);
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
        checkInputs(x, y);
        int bitLen = x[0].getNum();

        SquareShareZ2Vector cinVector = SquareShareZ2Vector.create(bitLen, cin);
        return addFull(x, y, cinVector);
    }

    // full n-bit adders
    public SquareShareZ2Vector[] addFull(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y, SquareShareZ2Vector cin) throws MpcAbortException {
        checkInputs(x, y);

        SquareShareZ2Vector[] res = new SquareShareZ2Vector[x.length + 1];
        SquareShareZ2Vector[] t = add(x[x.length - 1], y[y.length - 1], cin);
        res[res.length - 1] = t[S];
        for (int i = res.length - 1; i > 1; i--) {
            t = add(x[i - 2], y[i - 2], t[COUT]);
            res[i - 1] = t[S];
        }
        res[0] = t[COUT];
        return res;
    }

    public SquareShareZ2Vector[] addFull2(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y, boolean cin) throws MpcAbortException {
        int size = x.length;
        SquareShareZ2Vector axc, bxc, t;
        byte[] cinBytes = cin ? BytesUtils.not(new byte[x[0].getBytes().length], x[0].getNum()) : new byte[x[0].getBytes().length];

        SquareShareZ2Vector cinVector = SquareShareZ2Vector.create(
                BitVectorFactory.create(x[0].getNum(), cinBytes), true);
        // 如果不需要输出进位，则跳过最后一比特的AND运算
        int skipLast = 1;
        int i = 0;
        SquareShareZ2Vector[] res = new SquareShareZ2Vector[x.length + 1];
        while (size-- > skipLast) {
            axc = env.xor(x[i], cinVector);
            bxc = env.xor(y[i], cinVector);
            res[i] = env.xor(x[i], bxc);
            t = env.and(axc, bxc);
            cinVector = env.xor(cinVector, t);
            i++;
        }

        res[i] = env.xor(env.xor(cinVector, x[i]), y[i]);
        return Arrays.copyOf(res, x.length);
    }

    @Override
    public SquareShareZ2Vector[] sub(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) throws MpcAbortException {
        checkInputs(x, y);
        return add(x, not(y), true);
    }

    @Override
    public SquareShareZ2Vector leq(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) throws MpcAbortException {
        checkInputs(x, y);
        SquareShareZ2Vector[] result = sub(x, y);
        return result[0];
    }

    @Override
    public SquareShareZ2Vector[] bitAdd(SquareShareZ2Vector x, SquareShareZ2Vector y, SquareShareZ2Vector cin) throws MpcAbortException {
        return add(x, y, cin);
    }


    private void checkInputs(SquareShareZ2Vector[] x, SquareShareZ2Vector[] y) {
        Preconditions.checkArgument(x != null && y != null, "Null input data");
        Preconditions.checkArgument(x.length == y.length,
                "Array length of x and y not match");
    }

    public SquareShareZ2Vector[] not(SquareShareZ2Vector[] x) throws MpcAbortException {
        assert x != null;
        SquareShareZ2Vector[] result = new SquareShareZ2Vector[x.length];
        for (int i = 0; i < x.length; ++i) {
            result[i] = not(x[i]);
        }
        return result;
    }

    public SquareShareZ2Vector not(SquareShareZ2Vector x) throws MpcAbortException {
        assert x != null;

        SquareShareZ2Vector one = SquareShareZ2Vector.createOnes(x.getNum());
        return env.xor(x, one);
    }

}
