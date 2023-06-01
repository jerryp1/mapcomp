package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Manchester carry-chain adder.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class ManchesterAdder extends AbstractParallelPrefixAdder {

    public ManchesterAdder(MpcZ2cParty party) {
        super(party);
    }

    @Override
    public void addPrefix(Tuple[] tuples) throws MpcAbortException {
        Tuple input2 = tuples[l - 1];
        for (int i = l - 2; i >= 0; i--) {
            tuples[i] = op(tuples[i], input2);
            input2 = tuples[i];
        }
    }
}
