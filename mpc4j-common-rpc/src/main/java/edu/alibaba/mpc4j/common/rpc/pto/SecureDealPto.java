package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;

/**
 * Secure protocol with dealer.
 *
 * @author Weiran Liu
 * @date 2023/2/9
 */
public interface SecureDealPto extends SecurePto {
    /**
     * Gets the dealer's information.
     *
     * @return the dealer's information.
     */
    Party dealParty();
}
