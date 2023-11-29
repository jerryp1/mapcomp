package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory.ZlDreluType;

/**
 * DSZ15 Zl DReLU Config.
 *
 * @author Li Peng
 * @date 2023/11/18
 */
public class Dsz15ZlDreluConfig extends AbstractMultiPartyPtoConfig implements ZlDreluConfig {
    /**
     * A2b config
     */
    private final A2bConfig a2bConfig;
    /**
     * Z2 circuit config.
     */
    private final Z2cConfig z2cConfig;

    private Dsz15ZlDreluConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.a2bConfig, builder.z2cConfig);
        this.a2bConfig = builder.a2bConfig;
        this.z2cConfig = builder.z2cConfig;
    }

    public A2bConfig getA2bConfig() {
        return a2bConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public ZlDreluFactory.ZlDreluType getPtoType() {
        return ZlDreluType.DSZ15;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Dsz15ZlDreluConfig> {
        /**
         * A2b config
         */
        private final A2bConfig a2bConfig;
        /**
         * Z2 circuit config.
         */
        private final Z2cConfig z2cConfig;

        public Builder(Zl zl, boolean silent) {
            a2bConfig = A2bFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, zl,silent);
            z2cConfig = Z2cFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        @Override
        public Dsz15ZlDreluConfig build() {
            return new Dsz15ZlDreluConfig(this);
        }
    }
}
