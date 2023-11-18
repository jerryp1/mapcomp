package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.Arrays;

/**
 * Zl plain and party.
 *
 * @author Li Peng
 * @date 2023/11/8
 */
public interface PlainAndParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param x the input xi.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector and(BitVector x) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param x the input xi.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default SquareZ2Vector[] and(BitVector[] x) throws MpcAbortException {
        int[] nums = Arrays.stream(x).mapToInt(BitVector::bitNum).toArray();
        BitVector xMerge = BitVectorFactory.mergeWithPadding(x);
        return Arrays.stream(and(xMerge).getBitVector().splitWithPadding(nums))
            .map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
    }

    ;

}
