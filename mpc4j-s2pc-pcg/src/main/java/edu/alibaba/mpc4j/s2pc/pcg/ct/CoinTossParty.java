package edu.alibaba.mpc4j.s2pc.pcg.ct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * coin toss party.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public interface CoinTossParty extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxNum       max num of coins.
     * @param maxBitLength max bit length for each coin.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxNum, int maxBitLength) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num       num.
     * @param bitLength bit length for each coin.
     * @return coin-tossing result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] send(int num, int bitLength) throws MpcAbortException;
}
