package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Abstract Parallel Prefix Adder.
 * TODO 还需要加入各种检查
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public abstract class AbstractParallelPrefixAdder extends AbstractAdder {
    /**
     * bit length of input.
     */
    protected int l;

    public AbstractParallelPrefixAdder(MpcZ2cParty party) {
        super(party);
    }

    protected static class Tuple {
        /**
         * the generate bit.
         */
        private final MpcZ2Vector g;
        /**
         * the propagate bit.
         */
        private final MpcZ2Vector p;

        protected Tuple(MpcZ2Vector g, MpcZ2Vector p) {
            this.g = g;
            this.p = p;
        }

        public MpcZ2Vector getG() {
            return g;
        }

        public MpcZ2Vector getP() {
            return p;
        }
    }

    @Override
    public MpcZ2Vector[] add(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray, MpcZ2Vector cin)
            throws MpcAbortException {

        checkInputs(xiArray, yiArray);
        this.l = xiArray.length;
        MpcZ2Vector[] p = party.xor(xiArray, yiArray);
        MpcZ2Vector[] g = party.and(xiArray, yiArray);
        MpcZ2Vector[] c = new MpcZ2Vector[l];
        MpcZ2Vector[] s = new MpcZ2Vector[l + 1];
        Tuple[] tuples = IntStream.range(0, l)
                .mapToObj(i -> new Tuple(g[i], p[i])).toArray(Tuple[]::new);
        // add prefix
        addPrefix(tuples);

        // c TODO 这一步前后carry存在依赖关系，不是并行的
        for (int i = l - 1; i >= 0; i--) {
            if (i == l - 1) {
                c[i] = party.or(tuples[i].getG(), party.and(tuples[i].getP(), cin));
                continue;
            }
            c[i] = party.or(tuples[i].getG(), party.and(tuples[i].getP(), c[i + 1]));
        }
        // s
        for (int i = l; i >= 0; i--) {
            if (i == l) {
                s[i] = party.xor(p[i - 1], cin);
                continue;
            }
            if (i == 0) {
                s[i] = c[i];
                continue;
            }
            s[i] = party.xor(p[i - 1], c[i]);
        }
        return s;
    }

    public abstract void addPrefix(Tuple[] tuples) throws MpcAbortException;

    /**
     * Basic operation of parallel prefix adder, which is associative
     * and is able to be organized as parallel structure.
     *
     * @param input1 input tuple.
     * @param input2 input tuple.
     * @return output tuple.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected Tuple op(Tuple input1, Tuple input2) throws MpcAbortException {
        MpcZ2Vector gOut = party.or(input1.getG(), party.and(input1.getP(), input2.getG()));
        MpcZ2Vector pOut = party.and(input1.getP(), input2.getP());
        return new Tuple(gOut, pOut);
    }

    protected MpcZ2Vector[] extendsToCeil2(MpcZ2Vector[] x) {
        int num = x.length;
        int ceilBitLength = BigInteger.valueOf(num - 1).bitLength();
        int ceilNum = 1 << ceilBitLength;
        if (num != ceilNum) {
            MpcZ2Vector[] output = new MpcZ2Vector[ceilNum];
            System.arraycopy(x, 0, output, ceilNum - num, num);
            for (int i = 0; i < ceilNum - num; i++) {
                output[i] = party.createZeros(x[0].bitNum());
            }
            return output;
        }
        return x;
    }

    protected MpcZ2Vector[] cutToNum(MpcZ2Vector[] x, int num) {
        MathPreconditions.checkGreaterOrEqual("x.length", x.length, num);
        if (x.length != num) {
            return Arrays.copyOfRange(x, x.length - num, x.length);
        }
        return x;
    }
}
