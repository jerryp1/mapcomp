package edu.alibaba.mpc4j.common.circuit.z2.sorter;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Abstract Sorting Network.
 *
 * @author Li Peng
 * @date 2023/6/25
 */
public abstract class AbstractSortingNetwork extends AbstractSorter {
    /**
     * Z2 integer circuit.
     */
    private final Z2IntegerCircuit circuit;

    public AbstractSortingNetwork(Z2IntegerCircuit circuit) {
        super(circuit);
        this.circuit = circuit;
    }

    /**
     * Compare and exchange two items, exchange when order of xi, xj do not satisfies specified order.
     *
     * @param xiArray xiArray.
     * @param i       i.
     * @param j       j.
     * @param dir     sorting order.
     * @throws MpcAbortException the protocol failure aborts.
     */
    protected void exchangeWithOrder(MpcZ2Vector[][] xiArray, int i, int j, MpcZ2Vector dir) throws MpcAbortException {
        // exchange is ture when order of xi, xj do not satisfies dir
        MpcZ2Vector exchange = party.eq(party.not(circuit.leq(xiArray[i], xiArray[j])), dir);
        MpcZ2Vector[] s = mux(xiArray[j], xiArray[i], exchange);
        s = party.xor(s, xiArray[i]);
        MpcZ2Vector[] ki = party.xor(xiArray[j], s);
        MpcZ2Vector[] kj = party.xor(xiArray[i], s);
        xiArray[i] = ki;
        xiArray[j] = kj;
    }
}
