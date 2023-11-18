package edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.plainand.PlainAndFactory.PlainAndType;

/**
 * Plain and config.
 *
 * @author Li Peng
 * @date 2023/11/8
 */
public interface PlainAndConfig extends MultiPartyPtoConfig {
    /**
     * Gets the protocol type.
     *
     * @return the protocol type.
     */
    PlainAndType getPtoType();

    /**
     * Gets the zl.
     *
     * @return zl.
     */
    Zl getZl();
}
