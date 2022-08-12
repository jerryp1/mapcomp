package edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.BitOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.BitOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.bit2ot.nc.NcBitOtFactory.NcBitOtType;


/**
 * NC-BitOT协议发送方接口。
 *
 * @author Hanwen Feng
 * @date 2022/08/10
 */
public interface NcBitOtSender extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型
     *
     * @return 协议类型。
     */
    @Override
    NcBitOtType getPtoType();

    /**
     * 初始化协议。
     *
     * @param num 数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BitOtSenderOutput send() throws MpcAbortException;
}
