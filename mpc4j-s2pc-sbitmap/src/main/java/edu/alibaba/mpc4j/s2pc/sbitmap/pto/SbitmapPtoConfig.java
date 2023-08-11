package edu.alibaba.mpc4j.s2pc.sbitmap.pto;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * Sbitmap protocol config.
 *
 * @author Li Peng
 * @date 2023/8/3
 */
class SbitmapPtoConfig implements MultiPartyPtoConfig {

    public SbitmapPtoConfig() {
        // empty
    }

    @Override
    public void setEnvType(EnvType envType) {
        // do not need to set the environment
    }

    @Override
    public EnvType getEnvType() {
        return EnvType.STANDARD_JDK;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }
}

