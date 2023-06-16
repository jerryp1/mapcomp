package edu.alibaba.mpc4j.common.circuit.z2.sorter;

import edu.alibaba.mpc4j.common.circuit.z2.AbstractZ2Circuit;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Abstract Sorter
 *
 * @author Li Peng
 * @date 2023/6/12
 */
public abstract class AbstractSorter extends AbstractZ2Circuit implements Sorter {

    public AbstractSorter(Z2IntegerCircuit circuit) {
        super(circuit.getParty());
    }

    @Override
    public void sort(MpcZ2Vector[][] xiArrays) throws MpcAbortException {
        sort(xiArrays, party.createOnes(xiArrays[0][0].bitNum()));
    }
}
