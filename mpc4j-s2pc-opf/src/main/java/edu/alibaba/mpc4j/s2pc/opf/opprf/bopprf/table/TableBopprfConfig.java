package edu.alibaba.mpc4j.s2pc.opf.opprf.bopprf.table;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.opf.opprf.bopprf.BopprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.bopprf.BopprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;

/**
 * Table Batch OPRRF config.
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class TableBopprfConfig implements BopprfConfig {
    /**
     * OPRF config
     */
    private final OprfConfig oprfConfig;

    private TableBopprfConfig(Builder builder) {
        oprfConfig = builder.oprfConfig;
    }

    @Override
    public BopprfFactory.BopprfType getPtoType() {
        return BopprfFactory.BopprfType.TABLE;
    }

    @Override
    public void setEnvType(EnvType envType) {
        oprfConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return oprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<TableBopprfConfig> {
        /**
         * OPRF config
         */
        private OprfConfig oprfConfig;

        public Builder() {
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setOprfConfig(OprfConfig oprfConfig) {
            this.oprfConfig = oprfConfig;
            return this;
        }

        @Override
        public TableBopprfConfig build() {
            return new TableBopprfConfig(this);
        }
    }
}
