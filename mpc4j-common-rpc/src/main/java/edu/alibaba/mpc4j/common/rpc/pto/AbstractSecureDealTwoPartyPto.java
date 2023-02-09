package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;

/**
 * Abstract secure two-party computation with a trusted dealer.
 *
 * @author Weiran Liu
 * @date 2023/2/9
 */
public abstract class AbstractSecureDealTwoPartyPto extends AbstractSecureTwoPartyPto implements SecureDealPto {
    /**
     * the dealer's information.
     */
    private final Party dealParty;

    protected AbstractSecureDealTwoPartyPto(PtoDesc ptoDesc, Rpc rpc, Party otherParty, Party dealParty,
                                            SecurePtoConfig config) {
        super(ptoDesc, rpc, otherParty, config);
        this.dealParty = dealParty;
    }

    @Override
    public Party dealParty() {
        return dealParty;
    }
}
