package edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;

/**
 * Double PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/6/2
 */
public class Hhcm23DoubleSingleIndexPirConfig implements SingleIndexPirConfig {

    private EnvType envType;

    public Hhcm23DoubleSingleIndexPirConfig() {
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
    public SingleIndexPirFactory.SingleIndexPirType getProType() {
        return SingleIndexPirFactory.SingleIndexPirType.DOUBLE_PIR;
    }
}
