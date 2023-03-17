package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ideal;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgFactory;

/**
 * 理想核l比特三元组生成协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
public class IdealZlCoreMtgConfig implements ZlCoreMtgConfig {
    /**
     * the Zl instance
     */
    private final Zl zl;
    /**
     * 环境类型
     */
    private EnvType envType;

    private IdealZlCoreMtgConfig(Builder builder) {
        zl = builder.zl;
        envType = EnvType.STANDARD;
    }

    @Override
    public ZlCoreMtgFactory.ZlCoreMtgType getPtoType() {
        return ZlCoreMtgFactory.ZlCoreMtgType.IDEAL;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public int maxAllowNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.envType = envType;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.IDEAL;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<IdealZlCoreMtgConfig> {
        /**
         * the Zl instance
         */
        private final Zl zl;

        public Builder(Zl zl) {
            this.zl = zl;
        }

        @Override
        public IdealZlCoreMtgConfig build() {
            return new IdealZlCoreMtgConfig(this);
        }
    }
}
