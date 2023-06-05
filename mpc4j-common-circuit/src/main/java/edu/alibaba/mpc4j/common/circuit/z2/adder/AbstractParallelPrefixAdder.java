package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.stream.IntStream;

/**
 * Abstract Parallel Prefix Adder.
 * <p>
 * Parallel prefix adders are arguably the most commonly used arithmetic units in circuit design and have been extensively investigated in literature.
 * They are easy to pipeline and (part of them) enjoy lower circuit depth (compared with other adders), which is attracting to be used in MPC situation.
 * <p>
 * A taxonomy of parallel prefix adder can be found in following paper:
 *
 * <p>
 * Harris, David. "A taxonomy of parallel prefix networks." The Thrity-Seventh Asilomar Conference on Signals, Systems & Computers, 2003. Vol. 2. IEEE, 2003.
 * </p>
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public abstract class AbstractParallelPrefixAdder extends AbstractAdder {
    /**
     * bit length of input.
     */
    protected int l;
    /**
     * num
     */
    protected int num;

    public AbstractParallelPrefixAdder(MpcZ2cParty party) {
        super(party);
    }

    /**
     * The tuple consists of p and g bits, which are used in prefix network computation.
     */
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
        this.num = xiArray[0].getNum();
        // 1. pre-computation of g, p
        MpcZ2Vector[] p = party.xor(xiArray, yiArray);
        MpcZ2Vector[] g = party.and(xiArray, yiArray);
        Tuple[] tuples = IntStream.range(0, l)
                .mapToObj(i -> new Tuple(g[i], p[i])).toArray(Tuple[]::new);
        // 2. prefix computation using a prefix network
        addPrefix(tuples);
        // 3. carry-outs, c_i = (P_i · cin) + Gi
        MpcZ2Vector[] c = genCarryOuts(tuples, cin);
        // 4. s, the output sum bits, s_i = c_i ⊕ p_{i-1}
        return genSumOuts(p, c, cin);
    }

    /**
     * Generates the carry_out bits.
     *
     * @param tuples tuples.
     * @param cin    carry_in.
     * @return carry_outs.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private MpcZ2Vector[] genCarryOuts(Tuple[] tuples, MpcZ2Vector cin) throws MpcAbortException {
        MpcZ2Vector[] c = new MpcZ2Vector[l + 1];
        for (int i = l - 1; i >= 0; i--) {
            c[i] = party.or(tuples[i].getG(), party.and(tuples[i].getP(), cin));
        }
        return c;
    }

    /**
     * Generates the sum output bits.
     *
     * @param p   p
     * @param c   carry_outs
     * @param cin carry_in
     * @return sum output bits
     * @throws MpcAbortException the protocol failure aborts.
     */
    private MpcZ2Vector[] genSumOuts(MpcZ2Vector[] p, MpcZ2Vector[] c, MpcZ2Vector cin) throws MpcAbortException {
        MpcZ2Vector[] s = new MpcZ2Vector[l + 1];
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

    /**
     * Prefix computation using a prefix network.
     *
     * @param tuples tuples.
     * @throws MpcAbortException the protocol failure aborts.
     */
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
}
