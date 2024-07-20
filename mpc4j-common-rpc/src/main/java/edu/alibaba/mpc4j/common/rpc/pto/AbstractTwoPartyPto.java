package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

import java.util.List;

/**
 * Abstract two-party protocol.
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public abstract class AbstractTwoPartyPto extends AbstractMultiPartyPto implements TwoPartyPto {

    protected AbstractTwoPartyPto(PtoDesc ptoDesc, Rpc rpc, Party otherParty, MultiPartyPtoConfig config) {
        super(ptoDesc, config, rpc, otherParty);
    }

    @Override
    public Party otherParty() {
        return otherParties()[0];
    }

    /**
     * Sends payload to the other party.
     *
     * @param stepId  step ID.
     * @param payload payload.
     */
    protected void sendOtherPartyPayload(int stepId, List<byte[]> payload) {
        sendPayload(stepId, otherParty(), payload);
    }

    /**
     * Sends payload to the other party.
     *
     * @param stepId  step ID.
     * @param payload payload.
     */
    protected void sendOtherPartyEqualSizePayload(int stepId, List<byte[]> payload) {
        sendEqualSizePayload(stepId, otherParty(), payload);
    }

    /**
     * Receives payload from the other party.
     *
     * @param stepId step ID.
     * @return payload.
     */
    protected List<byte[]> receiveOtherPartyPayload(int stepId) {
        return receivePayload(stepId, otherParty());
    }

    /**
     * Receives payload from the other party, used in the protocols that single message may exceed 1GB
     *
     * @param stepId step ID.
     * @param num the number of arrays in the list
     * @param byteLength the byte length of each array
     * @return payload.
     */
    protected List<byte[]> receiveOtherPartyEqualSizePayload(int stepId, int num, int byteLength) {
        return receiveEqualSizePayload(stepId, otherParty(), num, byteLength);
    }
}
