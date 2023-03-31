package edu.alibaba.mpc4j.s2pc.aby.basics.bc.operator;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.matrix.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

/**
 * Receiver test thread for Boolean circuit unary operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class BcUnaryReceiverThread extends Thread {
    /**
     * sender
     */
    private final BcParty receiver;
    /**
     * operator
     */
    private final BcOperator bcOperator;
    /**
     * x bit vector
     */
    private final BitVector xBitVector;
    /**
     * number of bits
     */
    private final int bitNum;
    /**
     * share x1
     */
    private SquareSbitVector shareX1;
    /**
     * final x1
     */
    private SquareSbitVector finalX1;
    /**
     * z (plain)
     */
    private BitVector z1Vector;
    /**
     * z (secret)
     */
    private BitVector z0Vector;

    BcUnaryReceiverThread(BcParty receiver, BcOperator bcOperator, BitVector xBitVector) {
        this.receiver = receiver;
        this.bcOperator = bcOperator;
        this.xBitVector = xBitVector;
        bitNum = xBitVector.bitNum();
    }

    BitVector getZ1Vector() {
        return z1Vector;
    }

    BitVector getZ0Vector() {
        return z0Vector;
    }

    SquareSbitVector getShareX1() {
        return shareX1;
    }

    SquareSbitVector getFinalX1() {
        return finalX1;
    }

    @Override
    public void run() {
        try {
            receiver.init(bitNum, bitNum);
            // set inputs
            SquareSbitVector x = SquareSbitVector.create(xBitVector, true);
            SquareSbitVector x1 = receiver.shareOther(bitNum);
            shareX1 = x1.copy();
            //noinspection SwitchStatementWithTooFewBranches
            switch (bcOperator) {
                case NOT:
                    // (plain, plain)
                    SquareSbitVector z01 = receiver.not(x);
                    receiver.revealOther(z01);
                    z0Vector = receiver.revealOwn(z01);
                    // (plain, secret)
                    SquareSbitVector z11 = receiver.not(x1);
                    finalX1 = x1.copy();
                    receiver.revealOther(z11);
                    z1Vector = receiver.revealOwn(z11);
                    break;
                default:
                    throw new IllegalStateException("Invalid unary boolean operator: " + bcOperator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
