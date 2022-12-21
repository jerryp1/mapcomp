package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Boolean circuit party.
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public interface BcParty extends TwoPartyPto, SecurePto {
    /**
     * Get the protocol type.
     *
     * @return the protocol typ.e
     */
    @Override
    BcFactory.BcType getPtoType();

    /**
     * init the protocol.
     *
     * @param maxRoundBitNum maximum number of bits in round.
     * @param updateBitNum   total number of bits for updates.
     * @throws MpcAbortException if the protocol is abort.
     */
    void init(int maxRoundBitNum, int updateBitNum) throws MpcAbortException;

    /**
     * Share its own BitVector。
     *
     * @param x the BitVector to be shared.
     * @return the shared BitVector.
     */
    SquareSbitVector shareOwn(BitVector x);

    /**
     * Share other's BitVector.
     *
     * @param bitNum the number of bits to be shared.
     * @return the shared BitVector.
     * @throws MpcAbortException if the protocol is abort.
     */
    SquareSbitVector shareOther(int bitNum) throws MpcAbortException;

    /**
     * AND operation.
     *
     * @param xi xi,
     * @param yi yi,
     * @return zi, such that z0 ⊕ z1 = z = x & y = (x0 ⊕ x1) & (y0 ⊕ y1).
     * @throws MpcAbortException if the protocol is abort.
     */
    SquareSbitVector and(SquareSbitVector xi, SquareSbitVector yi) throws MpcAbortException;

    /**
     * XOR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z0 ⊕ z1 = z = x ^ y = (x0 ⊕ x1) ^ (y0 ⊕ y1)
     * @throws MpcAbortException if the protocol is abort.
     */
    SquareSbitVector xor(SquareSbitVector xi, SquareSbitVector yi) throws MpcAbortException;

    /**
     * OR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z0 ⊕ z1 = z = x | y = (x0 ⊕ x1) | (y0 ⊕ y1).
     * @throws MpcAbortException if the protocol is abort.
     */
    default SquareSbitVector or(SquareSbitVector xi, SquareSbitVector yi) throws MpcAbortException {
        return xor(xor(xi, yi), and(xi, yi));
    }

    /**
     * NOT operation.
     *
     * @param xi xi.
     * @return zi, such that z0 ⊕ z1 = z = !x = !(x0 ⊕ x1).
     * @throws MpcAbortException if the protocol is abort.
     */
    SquareSbitVector not(SquareSbitVector xi) throws MpcAbortException;

    /**
     * Reveal its own BitVector.
     *
     * @param xi the shared BitVector.
     * @return the reconstructed BitVector.
     * @throws MpcAbortException if the protocol is abort.
     */
    BitVector revealOwn(SquareSbitVector xi) throws MpcAbortException;

    /**
     * Reconstruct other's BitVector.
     *
     * @param xi the shared BitVector.
     */
    void revealOther(SquareSbitVector xi);

    /**
     * Get the number of input bits for secure boolean circuit computation.
     *
     * @param reset whether to reset the counter.
     * @return the number of input gits.
     */
    long inputBitNum(boolean reset);

    /**
     * Get the number of AND gates for secure boolean circuit computation.
     *
     * @param reset whether to reset the counter.
     * @return the number of AND gates.
     */
    long andGateNum(boolean reset);

    /**
     * Get the number of XOR gates for secure boolean circuit computation.
     *
     * @param reset whether to reset the counter.
     * @return the number of XOR gates for secure boolean circuit computation.
     */
    long xorGateNum(boolean reset);

    /**
     * Get the number of output bits for secure boolean circuit computation.
     *
     * @param reset whether to reset the counter.
     * @return the number of output gits.
     */
    long outputBitNum(boolean reset);
}
