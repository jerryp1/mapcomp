package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public interface SqOprfReceiver extends TwoPartyPto {

    /**
     * 初始化协议。
     *
     * @param maxBatchSize 最大批处理数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBatchSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param inputs 输入数组。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    SqOprfReceiverOutput oprf(byte[][] inputs) throws MpcAbortException;

}
