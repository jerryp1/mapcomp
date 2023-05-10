package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgFactory;

/**
 * DSZ15 HE-based Zl core multiplication triple generation protocol configuration.
 *
 * @author Li Peng, Weiran Liu
 * @date 2022/2/20
 */
public class Dsz15HeZlCoreMtgConfig implements ZlCoreMtgConfig {
    /**
     * the Zl instance
     */
    private final Zl zl;
    /**
     * the environment
     */
    private EnvType envType;

    private Dsz15HeZlCoreMtgConfig(Builder builder) {
        zl = builder.zl;
        envType = EnvType.STANDARD;
    }

    @Override
    public ZlCoreMtgFactory.ZlCoreMtgType getPtoType() {
        return ZlCoreMtgFactory.ZlCoreMtgType.DSZ15_HE;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public int maxNum() {
        return 1 << 20;
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
        return SecurityModel.SEMI_HONEST;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Dsz15HeZlCoreMtgConfig> {
        /**
         * the Zl instance
         */
        private final Zl zl;

        public Builder(Zl zl) {
            this.zl = zl;
        }

        @Override
        public Dsz15HeZlCoreMtgConfig build() {
            return new Dsz15HeZlCoreMtgConfig(this);
        }
    }
}
