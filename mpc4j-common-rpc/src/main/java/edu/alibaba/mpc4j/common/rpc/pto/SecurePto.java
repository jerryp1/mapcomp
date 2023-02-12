package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.tool.EnvType;

import java.security.SecureRandom;

/**
 * secure protocol.
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
public interface SecurePto {
    /**
     * Sets parallel computing.
     *
     * @param parallel parallel computing.
     */
    void setParallel(boolean parallel);

    /**
     * Gets parallel computing.
     *
     * @return parallel computing.
     */
    boolean getParallel();

    /**
     * Sets the secure random state.
     *
     * @param secureRandom the secure random state.
     */
    void setSecureRandom(SecureRandom secureRandom);

    /**
     * Gets environment.
     *
     * @return environment.
     */
    EnvType getEnvType();

    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    @SuppressWarnings("rawtypes")
    Enum getPtoType();
}
