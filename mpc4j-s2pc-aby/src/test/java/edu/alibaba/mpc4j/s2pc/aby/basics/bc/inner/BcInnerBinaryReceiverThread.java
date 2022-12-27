package edu.alibaba.mpc4j.s2pc.aby.basics.bc.inner;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

/**
 * Receiver test thread for Boolean circuit inner binary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BcInnerBinaryReceiverThread extends Thread {
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

    BcInnerBinaryReceiverThread(BcParty bcReceiver, BcOperator bcOperator, BitVector xBitVector, BitVector yBitVector) {
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
            SquareSbitVector xCopy = SquareSbitVector.create(xBitVector, true);
            SquareSbitVector x;
            SquareSbitVector y = SquareSbitVector.create(yBitVector, true);
            SquareSbitVector x1Copy = bcReceiver.shareOther(bitNum);
            SquareSbitVector x1;
            SquareSbitVector y1 = bcReceiver.shareOwn(yBitVector);
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    x = xCopy.copy();
                    bcReceiver.xori(x, y);
                    bcReceiver.revealOther(x);
                    z11 = bcReceiver.revealOwn(x);
                    // (plain, secret)
                    x = xCopy.copy();
                    bcReceiver.xori(x, y1);
                    bcReceiver.revealOther(x);
                    z10 = bcReceiver.revealOwn(x);
                    // (secret, plain)
                    x1 = x1Copy.copy();
                    bcReceiver.xori(x1, y);
                    bcReceiver.revealOther(x1);
                    z01 = bcReceiver.revealOwn(x1);
                    // (secret, secret)
                    x1 = x1Copy.copy();
                    bcReceiver.xori(x1, y1);
                    bcReceiver.revealOther(x1);
                    z00 = bcReceiver.revealOwn(x1);
                    break;
                case AND:
                    // (plain, plain)
                    x = xCopy.copy();
                    bcReceiver.andi(x, y);
                    bcReceiver.revealOther(x);
                    z11 = bcReceiver.revealOwn(x);
                    // (plain, secret)
                    x = xCopy.copy();
                    bcReceiver.andi(x, y1);
                    bcReceiver.revealOther(x);
                    z10 = bcReceiver.revealOwn(x);
                    // (secret, plain)
                    x1 = x1Copy.copy();
                    bcReceiver.andi(x1, y);
                    bcReceiver.revealOther(x1);
                    z01 = bcReceiver.revealOwn(x1);
                    // (secret, secret)
                    x1 = x1Copy.copy();
                    bcReceiver.andi(x1, y1);
                    bcReceiver.revealOther(x1);
                    z00 = bcReceiver.revealOwn(x1);
                    break;
                case OR:
                    // (plain, plain)
                    x = xCopy.copy();
                    bcReceiver.ori(x, y);
                    bcReceiver.revealOther(x);
                    z11 = bcReceiver.revealOwn(x);
                    // (plain, secret)
                    x = xCopy.copy();
                    bcReceiver.ori(x, y1);
                    bcReceiver.revealOther(x);
                    z10 = bcReceiver.revealOwn(x);
                    // (secret, plain)
                    x1 = x1Copy.copy();
                    bcReceiver.ori(x1, y);
                    bcReceiver.revealOther(x1);
                    z01 = bcReceiver.revealOwn(x1);
                    // (secret, secret)
                    x1 = x1Copy.copy();
                    bcReceiver.ori(x1, y1);
                    bcReceiver.revealOther(x1);
                    z00 = bcReceiver.revealOwn(x1);
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
