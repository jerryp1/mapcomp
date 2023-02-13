package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;

/**
 * 核布尔三元组生成协议接口。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public interface ZlCoreMtgParty extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    ZlTriple generate(int num) throws MpcAbortException;
}
