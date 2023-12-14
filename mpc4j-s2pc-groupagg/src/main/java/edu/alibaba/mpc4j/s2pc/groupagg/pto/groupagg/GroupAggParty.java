package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.Properties;

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
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Properties properties) throws MpcAbortException;

    /**
     * Group aggregation.
     *
     * @param groupField group field.
     * @param aggField   aggregation field.
     * @return result.
     */
    GroupAggOut groupAgg(String[] groupField, long[] aggField, SquareZ2Vector e) throws MpcAbortException;

    long getGroupStep1Time();

    long getGroupStep2Time();

    long getGroupStep3Time();

    long getGroupStep4Time();

    long getGroupStep5Time();

    long getAggTime();

    long getGroupTripleNum();

    long getAggTripleNum();
}
