package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * circuit PSI config, where server encodes payload into circuit
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public interface PlpsiConfig extends MultiPartyPtoConfig {
    /**
     * Gets the type.
     *
     * @return tye type.
     */
    PlpsiFactory.PlpsiType getPtoType();

    /**
     * Gets number of shared bits.
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @return number of shared bits.
     */
    int getOutputBitNum(int serverElementSize, int clientElementSize);

    /**
     * Gets the share type of payload.
     *
     * @return true if the payload is binary shared, false if the payload is arithmetic shared
     */
    boolean isBinaryShare();
}
