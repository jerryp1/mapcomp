package edu.alibaba.mpc4j.s2pc.opf.groupagg;

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
//
//    int getStep1Time();
//
//    int getStep2Time();
//
//    int getStep3Time();
//
//    int getStep4Time();
//
//    int getStep5Time();
//
//    int getAggTime();
}
