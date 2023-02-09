package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PartyState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * Multi-party protocol.
 *
 * @author Weiran Liu
 * @date 2021/12/19
 */
public interface MultiPartyPto {

    /**
     * Sets task ID.
     *
     * @param taskId task ID.
     */
    void setTaskId(long taskId);

    /**
     * Gets task ID.
     *
     * @return task ID.
     */
    long getTaskId();

    /**
     * Gets the invoked rpc instance.
     *
     * @return the invoked rpc instance.
     */
    Rpc getRpc();

    /**
     * Gets its own party information.
     *
     * @return its own party information.
     */
    default Party ownParty() {
        return getRpc().ownParty();
    }

    /**
     * Gets the protocol description.
     *
     * @return the protocol description.
     */
    PtoDesc getPtoDesc();

    /**
     * Adds the log level.
     */
    void addLogLevel();

    /**
     * Gets other parties' information.
     *
     * @return other parties' information.
     */
    Party[] otherParties();

    /**
     * Gets party state.
     *
     * @return party state.
     */
    PartyState getPartyState();

    /**
     * Destroys the protocol.
     */
    void destroy();
}
