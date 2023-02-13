package edu.alibaba.mpc4j.s2pc.aby.circuit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * 整数运算参与方。
 *
 * @author Weiran Liu
 * @date 2022/12/13
 */
public interface IntegerParty extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxBatchNum 最大批处理数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBatchNum) throws MpcAbortException;


}
