package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.BitOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.NcBitOtFactory.NcBitOtType;
/**
 * NC-BitOT协议接收方接口。
 *
 * @author Hanwen Feng
 * @date 2022/08/10
 */
public interface NcBitOtReceiver extends TwoPartyPto, SecurePto {
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
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BitOtReceiverOutput receive() throws MpcAbortException;
}