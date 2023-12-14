package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapFactory.PmapType;

/**
 * PMAP protocol configure interface
 *
 * @author Feng Han
 * @date 2023/10/23
 */
public interface PmapConfig extends MultiPartyPtoConfig {
    /**
     * return the type of protocol
     *
     * @return protocol type
     */
    PmapType getPtoType();
}
