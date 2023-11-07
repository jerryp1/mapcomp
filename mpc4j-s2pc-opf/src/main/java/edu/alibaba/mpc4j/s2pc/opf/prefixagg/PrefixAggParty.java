package edu.alibaba.mpc4j.s2pc.opf.prefixagg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.util.Vector;

/**
 * Prefix sum Interface.
 *
 * @author Li Peng
 * @date 2023/10/30
 */
public interface PrefixAggParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxL   max l.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PrefixAggOutput agg(Vector<byte[]> groupField, SquareZlVector sumField) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param groupIndicator1 (group_i != group_{i-1}), such as 10001000.
     * @param groupIndicator2 (group_i == group_{i+1}), such as 11101110.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZlVector agg(SquareZ2Vector groupIndicator1, SquareZ2Vector groupIndicator2, SquareZlVector sumField) throws MpcAbortException;

    /**
     * obtain a boolean indicator to indicate whether (group_i != group_{i-1}), such as 10001000.
     *
     * @param groupField grouping field.
     * @return a boolean indicator.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector obtainGroupIndicator1(Vector<byte[]> groupField) throws MpcAbortException;

    /**
     * obtain a boolean indicator to indicate whether (group_i == group_{i+1}), such as 11101110.
     *
     * @param groupField grouping field.
     * @return a boolean indicator.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZ2Vector obtainGroupIndicator2(Vector<byte[]> groupField) throws MpcAbortException;
}
