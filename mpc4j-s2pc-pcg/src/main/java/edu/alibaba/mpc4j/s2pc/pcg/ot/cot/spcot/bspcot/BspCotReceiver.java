package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.spcot.bspcot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.spcot.bspcot.BspCotFactory.BspCotType;

/**
 * BSP-COT接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface BspCotReceiver extends TwoPartyPto, SecurePto {

    @Override
    BspCotType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxBatch 最大批处理数量。
     * @param maxNum   最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBatch, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alphaArray α数组。
     * @param num        数量。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BspCotReceiverOutput receive(int[] alphaArray, int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alphaArray        α数组。
     * @param num               数量。
     * @param preReceiverOutput 预计算接收方输出。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BspCotReceiverOutput receive(int[] alphaArray, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}
