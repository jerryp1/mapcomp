package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.hardcode;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory.Z2MtgType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;

/**
 * cache Z2 multiplication triple generator config.
 *
 * @author Li Peng
 * @date 2023/11/11
 */
public class HardcodeZ2MtgConfig extends AbstractMultiPartyPtoConfig implements Z2MtgConfig {

    private HardcodeZ2MtgConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
    }

    @Override
    public Z2MtgFactory.Z2MtgType getPtoType() {
        return Z2MtgType.HARDCODE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<HardcodeZ2MtgConfig> {
        /**
         * core multiplication triple generator config
         */
        private Z2CoreMtgConfig coreMtgConfig;

        public Builder(SecurityModel securityModel) {
            coreMtgConfig = Z2CoreMtgFactory.createDefaultConfig(securityModel, true);
        }

        public Builder setCoreMtgConfig(Z2CoreMtgConfig coreMtgConfig) {
            this.coreMtgConfig = coreMtgConfig;
            return this;
        }

        @Override
        public HardcodeZ2MtgConfig build() {
            return new HardcodeZ2MtgConfig(this);
        }
    }
}
