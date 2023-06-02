package edu.alibaba.mpc4j.common.circuit.z2.adder;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;

/**
 * Parallel prefix adder using Brent-Kung structure.
 *
 * @author Li Peng
 * @date 2023/6/1
 */
public class BrentKungAdder extends AbstractParallelPrefixAdder {

    public BrentKungAdder(MpcZ2cParty party) {
        super(party);
    }

    @Override
    public void addPrefix(Tuple[] tuples) throws MpcAbortException {
        int ceilL = 1 << (BigInteger.valueOf(tuples.length - 1).bitLength());
        // offset denotes the distance of index in a perfect binary tree (with ceilL leaves) and index in the ture tree (with l nodes).
        int offset = ceilL - l;
        int logL = LongUtils.ceilLog2(ceilL);
        int blockNum = ceilL / 2;
        int blockSize = 2;
        // reduction will be performed in a perfect binary tree (with ceilL leaves) instead of the ture tree,
        // while we should avoid iterations in the nodes which beyond ture indexes by determining if index >= 0.
        // first tree-reduction
        for (int i = 0; i < logL; i++) {
            for (int j = 0; j < blockNum; j++) {
                int inputIndex = ceilL - (j * blockSize + blockSize / 2) - offset;
                if (inputIndex >= 0) {
                    Tuple input = tuples[inputIndex];
                    int currentIndex = ceilL - ((j + 1) * blockSize) - offset;
                    if (currentIndex >= 0) {
                        tuples[currentIndex] = op(tuples[currentIndex], input);
                    }
                }
            }
            blockNum >>= 1;
            blockSize <<= 1;
        }
        // second tree-reduction
        blockNum = 2;
        blockSize = ceilL / 2;
        for (int i = 0; i < logL - 1; i++) {
            for (int j = 0; j < blockNum - 1; j++) {
                int inputIndex = ceilL - (j + 1) * blockSize - offset;
                if (inputIndex >= 0) {
                    Tuple input = tuples[ceilL - (j + 1) * blockSize - offset];
                    int current = ceilL - (j + 1) * blockSize - blockSize / 2 - offset;
                    if (current >= 0) {
                        tuples[current] = op(tuples[current], input);
                    }
                }
            }
            blockNum <<= 1;
            blockSize >>= 1;
        }
    }
}
