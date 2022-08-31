package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotReceiverOutput;



/**
 * No Choice 2^l选1-OT接收方。
 *
 * @author  Hanwen Feng
 * @date 2022/08/16
 */
public interface NcLotReceiver extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     * @return 协议类型。
     */
    @Override
    NcLotFactory.NcLotType getPtoType();


    /**
     * 初始化协议。
     *
     * @param inputBitLength  输入比特长度
     * @param num 执行数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int inputBitLength, int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     * @return 接收方输出。
     */
    LotReceiverOutput receive() throws MpcAbortException;
}
