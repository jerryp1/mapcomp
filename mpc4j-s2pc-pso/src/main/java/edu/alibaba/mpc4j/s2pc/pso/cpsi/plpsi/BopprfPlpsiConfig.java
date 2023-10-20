package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfConfig;

/**
 * batched OPPRF-based payload-circuit PSI config.
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public interface BopprfPlpsiConfig extends PlpsiConfig {
    /**
     * Gets batched OPPRF config.
     *
     * @return batched OPPRF config.
     */
    BopprfConfig getBopprfConfig();

    /**
     * Gets PEQT config.
     *
     * @return PEQT config.
     */
    PeqtConfig getPeqtConfig();

    /**
     * Gets cuckoo hash bin type.
     *
     * @return cuckoo hash bin type.
     */
    CuckooHashBinType getCuckooHashBinType();
}
