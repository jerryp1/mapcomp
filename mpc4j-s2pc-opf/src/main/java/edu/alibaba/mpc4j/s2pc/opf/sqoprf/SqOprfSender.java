package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public interface SqOprfSender extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxBatchSize 最大批处理数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBatchSize) throws MpcAbortException;


    void init(int maxBatchSize, SqOprfSenderKey key) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param batchSize 批处理数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    SqOprfSenderOutput oprf(int batchSize) throws MpcAbortException;
}
