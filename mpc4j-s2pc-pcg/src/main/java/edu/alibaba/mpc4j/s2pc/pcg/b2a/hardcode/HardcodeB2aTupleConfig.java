package edu.alibaba.mpc4j.s2pc.pcg.b2a.hardcode;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.b2a.B2aTupleFactory.B2aTupleType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;

/**
 * cache Z2 multiplication triple generator config.
 *
 * @author Li Peng
 * @date 2023/11/21
 */
public class HardcodeB2aTupleConfig extends AbstractMultiPartyPtoConfig implements B2aTupleConfig {
    /**
     * zl
     */
    private Zl zl;

    private HardcodeB2aTupleConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        zl = builder.zl;
    }

    @Override
    public B2aTupleType getPtoType() {
        return B2aTupleType.HARDCODE;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<HardcodeB2aTupleConfig> {
        /**
         * zl
         */
        private Zl zl;

        public Builder(Zl zl) {
            this.zl = zl;
        }

        public Builder setCoreMtgConfig(Z2CoreMtgConfig coreMtgConfig) {
            return this;
        }

        @Override
        public HardcodeB2aTupleConfig build() {
            return new HardcodeB2aTupleConfig(this);
        }
    }
}
