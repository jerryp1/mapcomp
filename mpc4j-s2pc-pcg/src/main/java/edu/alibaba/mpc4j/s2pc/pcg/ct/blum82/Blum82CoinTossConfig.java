package edu.alibaba.mpc4j.s2pc.pcg.ct.blum82;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;

/**
 * Blum82 coin-tossing protocol config.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public class Blum82CoinTossConfig implements CoinTossConfig {
    /**
     * environment
     */
    private EnvType envType;

    private Blum82CoinTossConfig(Builder builder) {
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
        return SecurityModel.MALICIOUS;
    }

    @Override
    public CoinTossFactory.CoinTossType getPtoType() {
        return CoinTossFactory.CoinTossType.BLUM82;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Blum82CoinTossConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Blum82CoinTossConfig build() {
            return new Blum82CoinTossConfig(this);
        }
    }
}
