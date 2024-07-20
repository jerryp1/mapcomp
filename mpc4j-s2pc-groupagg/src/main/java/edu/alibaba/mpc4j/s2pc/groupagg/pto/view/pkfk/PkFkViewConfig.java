package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewFactory.PtoType;

/**
 * view config
 *
 * @author Feng Han
 * @date 2024/7/19
 */
public interface PkFkViewConfig extends MultiPartyPtoConfig {
    /**
     * protocol type
     */
    PtoType getPtoType();
}