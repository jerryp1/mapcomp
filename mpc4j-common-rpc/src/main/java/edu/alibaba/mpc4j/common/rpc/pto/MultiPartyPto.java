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
     * Sets the task ID.
     *
     * @param taskId the task ID.
     */
    void setTaskId(int taskId);

    /**
     * Gets the task ID.
     *
     * @return the task ID.
     */
    int getTaskId();

    /**
     * Sets the encoded task ID.
     *
     * @param taskId       the task ID.
     * @param parentTreeId the parent tree ID.
     */
    void setEncodeTaskId(int taskId, int parentTreeId);

    /**
     * Adds the tree level.
     *
     * @param rowLevel     row level.
     * @param taskId       the task ID.
     * @param parentTreeId the parent tree ID.
     */
    void addTreeLevel(int rowLevel, int taskId, int parentTreeId);

    /**
     * Gets the encoded task ID.
     *
     * @return the encoded task ID.
     */
    long getEncodeTaskId();

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
