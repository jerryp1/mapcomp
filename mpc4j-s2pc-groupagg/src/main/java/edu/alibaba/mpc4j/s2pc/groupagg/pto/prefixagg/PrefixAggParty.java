package edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.prefixagg.PrefixAggFactory.PrefixAggTypes;

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
     * @param groupField the field of group by.
     * @param aggField   the field of aggregatioin.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PrefixAggOutput agg(Vector<byte[]> groupField, SquareZlVector aggField) throws MpcAbortException;

    /**
     * Executes the protocol. Assume groupField is hold by receiver.
     *
     * @param groupField the field of group by.
     * @param aggField   the field of aggregatioin.
     * @param flag       the flag to indicate the validity of elements.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PrefixAggOutput agg(Vector<byte[]> groupField, SquareZlVector aggField, SquareZ2Vector flag) throws MpcAbortException;


    /**
     * Executes the protocol. Assume groupField is hold by receiver.
     *
     * @param groupField the field of group by.
     * @param aggField   the field of aggregatioin.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PrefixAggOutput agg(String[] groupField, SquareZlVector aggField) throws MpcAbortException;

    /**
     * Executes the protocol. Assume groupField is hold by receiver.
     *
     * @param groupField the field of group by.
     * @param aggField   the field of aggregatioin.
     * @param flag       the flag to indicate the validity of elements.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PrefixAggOutput agg(String[] groupField, SquareZlVector aggField, SquareZ2Vector flag) throws MpcAbortException;


    /**
     * Executes the protocol.
     *
     * @param groupField the field of group by.
     * @param aggField   the field of aggregatioin.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PrefixAggOutput agg(Vector<byte[]> groupField, SquareZ2Vector[] aggField) throws MpcAbortException;

    /**
     * Executes the protocol. Assume groupField is hold by receiver.
     *
     * @param groupField the field of group by.
     * @param aggField   the field of aggregatioin.
     * @param flag       the flag to indicate the validity of elements.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PrefixAggOutput agg(Vector<byte[]> groupField, SquareZ2Vector[] aggField, SquareZ2Vector flag) throws MpcAbortException;


    /**
     * Executes the protocol. Assume groupField is hold by receiver.
     *
     * @param groupField the field of group by.
     * @param aggField   the field of aggregatioin.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PrefixAggOutput agg(String[] groupField, SquareZ2Vector[] aggField) throws MpcAbortException;

    /**
     * Executes the protocol. Assume groupField is hold by receiver.
     *
     * @param groupField the field of group by.
     * @param aggField   the field of aggregatioin.
     * @param flag       the flag to indicate the validity of elements.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    PrefixAggOutput agg(String[] groupField, SquareZ2Vector[] aggField, SquareZ2Vector flag) throws MpcAbortException;

    PrefixAggTypes getAggType();

}
