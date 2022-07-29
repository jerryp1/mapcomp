package edu.alibaba.mpc4j.s2pc.pcg.btg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BtgFactory.BtgType;

/**
 * BTG协议接收方接口。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/02/07
 */
public interface BtgParty extends TwoPartyPto, SecurePto {

    @Override
    BtgType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxRoundNum 最大单轮数量。
     * @param updateNum   更新数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxRoundNum, int updateNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 布尔三元组数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BooleanTriple generate(int num) throws MpcAbortException;
}
