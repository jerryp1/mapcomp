package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk.PkFkViewFactory.ViewPtoType;

/**
 * view config
 *
 */
public interface PkFkViewConfig extends MultiPartyPtoConfig {
    /**
     * protocol type
     */
    ViewPtoType getPtoType();
}