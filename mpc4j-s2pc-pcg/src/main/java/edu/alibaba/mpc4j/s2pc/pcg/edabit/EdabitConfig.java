package edu.alibaba.mpc4j.s2pc.pcg.edabit;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.edabit.EdabitFactory.EdabitType;

/**
 * Zl下EdaBit生成的配置
 *
 * @author Xinwei Gao
 * Date: 2023-04-04
 */
public interface EdabitConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    EdabitType getPtoType();
}
