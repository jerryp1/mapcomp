package edu.alibaba.mpc4j.s2pc.pcg.dpprf.gf2k;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * GF2K-DPPRF协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public interface Gf2kDpprfSender extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    Gf2kDpprfFactory.Gf2kDpprfType getPtoType();

    /**
     * 初始化协议。
     *
     * @param delta         关联值Δ。
     * @param maxBatchNum   最大批处理数量。
     * @param maxAlphaBound 最大α上界。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(byte[] delta, int maxBatchNum, int maxAlphaBound) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param batchNum   批处理数量。
     * @param alphaBound α上界。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Gf2kDpprfSenderOutput puncture(int batchNum, int alphaBound) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param batchNum        批处理数量。
     * @param alphaBound      α上界。
     * @param preSenderOutput 预计算发送方输出。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Gf2kDpprfSenderOutput puncture(int batchNum, int alphaBound, CotSenderOutput preSenderOutput) throws MpcAbortException;
}
