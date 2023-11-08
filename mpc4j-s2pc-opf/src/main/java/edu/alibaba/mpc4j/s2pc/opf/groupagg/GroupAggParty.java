package edu.alibaba.mpc4j.s2pc.opf.groupagg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * Group-Aggregation party
 *
 * @author Li Peng
 * @date 2023/11/3
 */
public interface GroupAggParty extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxL   max l.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int maxNum) throws MpcAbortException;

    /**
     * Group aggregation.
     *
     * @param groupField group field.
     * @param aggField   aggregation field.
     * @return result.
     */
    GroupAggOut groupAgg(String[][] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException;
}
