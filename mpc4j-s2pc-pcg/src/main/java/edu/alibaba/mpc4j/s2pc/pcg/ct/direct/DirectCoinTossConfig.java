package edu.alibaba.mpc4j.s2pc.pcg.ct.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;

/**
 * direct coin-tossing protocol config.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public class DirectCoinTossConfig implements CoinTossConfig {
    /**
     * environment
     */
    private EnvType envType;

    private DirectCoinTossConfig(Builder builder) {
        envType = EnvType.STANDARD;
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

    @Override
    public CoinTossFactory.CoinTossType getPtoType() {
        return CoinTossFactory.CoinTossType.DIRECT;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectCoinTossConfig> {

        public Builder() {
            // empty
        }

        @Override
        public DirectCoinTossConfig build() {
            return new DirectCoinTossConfig(this);
        }
    }
}
