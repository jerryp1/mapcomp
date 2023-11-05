package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory.FieldTypes;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory.PermGenTypes;

public interface PermGenConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PermGenTypes getPtoType();

    /**
     * Get the zl.
     *
     * @return the zl.
     */
    Zl getZl();

    FieldTypes getFieldType();
}
