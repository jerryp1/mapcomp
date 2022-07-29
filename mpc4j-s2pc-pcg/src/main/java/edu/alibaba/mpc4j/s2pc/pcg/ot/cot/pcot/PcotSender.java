package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pcot.PcotFactory.PcotType;

/**
 * PCOT协议发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface PcotSender extends TwoPartyPto, SecurePto {

    @Override
    PcotType getPtoType();

    /**
     * 初始化协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init() throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param preSenderOutput 预计算发送方输出。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    CotSenderOutput send(CotSenderOutput preSenderOutput) throws MpcAbortException;
}
