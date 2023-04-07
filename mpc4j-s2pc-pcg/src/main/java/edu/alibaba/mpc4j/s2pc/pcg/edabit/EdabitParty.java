package edu.alibaba.mpc4j.s2pc.pcg.edabit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * ZlEdabit生成协议参与方接口
 *
 * @author Xinwei Gao
 * Date: 2023-04-04
 */
public interface EdabitParty extends TwoPartyPto {
    /**
     * 初始化协议
     *
     * @throws MpcAbortException
     */
    void init() throws MpcAbortException;

    /**
     * 执行协议
     *
     * @param num Edabit数量
     *
     * @return 参与方输出
     *
     * @throws MpcAbortException 如果协议异常中止
     */
    Edabit generate(int num) throws MpcAbortException;
}
