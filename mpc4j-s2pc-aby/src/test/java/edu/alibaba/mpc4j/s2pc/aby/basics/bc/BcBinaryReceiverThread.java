package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Receiver test thread for Boolean circuit binary operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class BcBinaryReceiverThread extends Thread {
    /**
     * receiver
     */
    private final BcParty bcReceiver;
    /**
     * operator
     */
    private final BcOperator bcOperator;
    /**
     * x bit vector
     */
    private final BitVector xBitVector;
    /**
     * y bit vector
     */
    private final BitVector yBitVector;
    /**
     * number of bits
     */
    private final int bitNum;
    /**
     * z (plain, plain)
     */
    private BitVector z11;
    /**
     * z (plain, secret)
     */
    private BitVector z10;
    /**
     * z (secret, plain)
     */
    private BitVector z01;
    /**
     * zi (secret, secret)
     */
    private BitVector z00;

    BcBinaryReceiverThread(BcParty bcReceiver, BcOperator bcOperator, BitVector xBitVector, BitVector yBitVector) {
        this.bcReceiver = bcReceiver;
        this.bcOperator = bcOperator;
        assert xBitVector.bitNum() == yBitVector.bitNum();
        this.xBitVector = xBitVector;
        this.yBitVector = yBitVector;
        bitNum = xBitVector.bitNum();
    }

    BitVector getZ11() {
        return z11;
    }

    BitVector getZ10() {
        return z10;
    }

    BitVector getZ01() {
        return z01;
    }

    BitVector getZ00() {
        return z00;
    }

    @Override
    public void run() {
        try {
            bcReceiver.getRpc().connect();
            bcReceiver.init(bitNum, bitNum);
            // set inputs
            SquareSbitVector x = SquareSbitVector.create(xBitVector, true);
            SquareSbitVector y = SquareSbitVector.create(yBitVector, true);
            SquareSbitVector x1 = bcReceiver.shareOther(bitNum);
            SquareSbitVector y1 = bcReceiver.shareOwn(yBitVector);
            SquareSbitVector z111, z101, z011, z001;
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    z111 = bcReceiver.xor(x, y);
                    bcReceiver.revealOther(z111);
                    z11 = bcReceiver.revealOwn(z111);
                    // (plain, secret)
                    z101 = bcReceiver.xor(x, y1);
                    bcReceiver.revealOther(z101);
                    z10 = bcReceiver.revealOwn(z101);
                    // (secret, plain)
                    z011 = bcReceiver.xor(x1, y);
                    bcReceiver.revealOther(z011);
                    z01 = bcReceiver.revealOwn(z011);
                    // (secret, secret)
                    z001 = bcReceiver.xor(x1, y1);
                    bcReceiver.revealOther(z001);
                    z00 = bcReceiver.revealOwn(z001);
                    break;
                case AND:
                    // (plain, plain)
                    z111 = bcReceiver.and(x, y);
                    bcReceiver.revealOther(z111);
                    z11 = bcReceiver.revealOwn(z111);
                    // (plain, secret)
                    z101 = bcReceiver.and(x, y1);
                    bcReceiver.revealOther(z101);
                    z10 = bcReceiver.revealOwn(z101);
                    // (secret, plain)
                    z011 = bcReceiver.and(x1, y);
                    bcReceiver.revealOther(z011);
                    z01 = bcReceiver.revealOwn(z011);
                    // (secret, secret)
                    z001 = bcReceiver.and(x1, y1);
                    bcReceiver.revealOther(z001);
                    z00 = bcReceiver.revealOwn(z001);
                    break;
                case OR:
                    // (plain, plain)
                    z111 = bcReceiver.or(x, y);
                    bcReceiver.revealOther(z111);
                    z11 = bcReceiver.revealOwn(z111);
                    // (plain, secret)
                    z101 = bcReceiver.or(x, y1);
                    bcReceiver.revealOther(z101);
                    z10 = bcReceiver.revealOwn(z101);
                    // (secret, plain)
                    z011 = bcReceiver.or(x1, y);
                    bcReceiver.revealOther(z011);
                    z01 = bcReceiver.revealOwn(z011);
                    // (secret, secret)
                    z001 = bcReceiver.or(x1, y1);
                    bcReceiver.revealOther(z001);
                    z00 = bcReceiver.revealOwn(z001);
                    break;
                default:
                    throw new IllegalStateException("Invalid binary boolean operator: " + bcOperator.name());
            }
            bcReceiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
