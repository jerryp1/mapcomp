package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * Parallel Prefix Adder using Sklansky structure.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class SklanskyAdder extends AbstractParallelPrefixAdder {

    public SklanskyAdder(MpcZ2cParty party) {
        super(party);
    }

    @Override
    public void addPrefix(Tuple[] tuples) throws MpcAbortException {
        int logL = LongUtils.ceilLog2(l);
        int blockNum = l / 2;
        int blockSize = 2;
        for (int i = 0; i < logL; i++) {
            for (int j = 0; j < blockNum; j++) {
                Tuple input2 = tuples[l - (j * blockSize + blockSize / 2)];
                for (int k = 0; k < blockSize / 2; k++) {
                    int current = l - (j * blockSize + blockSize / 2 + k) - 1;
                    tuples[current] = op(tuples[current], input2);
                }
            }
            blockNum = (blockNum >> 1);
            blockSize = (blockSize << 1);
        }
    }
}
