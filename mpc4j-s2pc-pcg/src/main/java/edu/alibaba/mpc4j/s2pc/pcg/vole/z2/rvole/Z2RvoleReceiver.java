package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.rvole;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;

/**
 * Z_2-RVOLE协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/6/12
 */
public interface Z2RvoleReceiver extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    Z2RvoleFactory.Z2RvoleType getPtoType();

    /**
     * 初始化协议。
     *
     * @param delta 关联值Δ。
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(boolean delta, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 数量。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2VoleReceiverOutput receive(int num) throws MpcAbortException;
}