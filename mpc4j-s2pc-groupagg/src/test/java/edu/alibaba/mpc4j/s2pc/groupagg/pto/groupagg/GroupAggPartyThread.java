package edu.alibaba.mpc4j.s2pc.groupagg.pto.groupagg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.Properties;

/**
 * Group aggregation party thread.
 *
 * @author Li Peng
 * @date 2023/11/9
 */
class GroupAggPartyThread extends Thread {
    /**
     * the sender
     */
    private final GroupAggParty groupAggParty;
    /**
     * groups
     */
    private final String[] groups;
    /**
     * payload
     */
    private final long[] payload;
    /**
     * z
     */
    private GroupAggOut output;
    /**
     * Property
     */
    private final Properties properties;

    private SquareZ2Vector e;

    GroupAggPartyThread(GroupAggParty groupAggParty, String[] groups, long[] payload, SquareZ2Vector e, Properties properties) {
        this.groupAggParty = groupAggParty;
        this.groups = groups;
        this.payload = payload;
        this.properties = properties;
        this.e = e;
    }

    GroupAggOut getOutput() {
        return output;
    }

    @Override
    public void run() {
        try {
            groupAggParty.init(properties);
            output = groupAggParty.groupAgg(groups, payload, e);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
