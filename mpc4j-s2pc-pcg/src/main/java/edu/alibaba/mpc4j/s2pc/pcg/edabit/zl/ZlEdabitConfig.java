package edu.alibaba.mpc4j.s2pc.pcg.edabit.zl;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.edabit.EdabitConfig;
import edu.alibaba.mpc4j.s2pc.pcg.edabit.EdabitFactory.EdabitType;

/**
 * Zl下Edabit的配置
 *
 * @author qiyuan.gxw
 * Date: 2023-04-04
 */
public class ZlEdabitConfig implements EdabitConfig {

    @Override
    public void setEnvType(EnvType envType) {

    }

    @Override
    public EnvType getEnvType() {
        return null;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    @Override
    public EdabitType getPtoType() {
        return EdabitType.ZL_EDABIT;
    }
}
