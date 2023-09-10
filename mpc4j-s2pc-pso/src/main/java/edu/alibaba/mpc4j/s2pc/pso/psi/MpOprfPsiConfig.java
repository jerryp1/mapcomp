package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;

/**
 * mp-OPRF-based PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public interface MpOprfPsiConfig extends PsiConfig {
    /**
     * Gets mp-OPRF config.
     *
     * @return mp-OPRF config.
     */
    MpOprfConfig getMpOprfConfig();

    /**
     * Gets filter type.
     *
     * @return filter type.
     */
    FilterType getFilterType();
}
