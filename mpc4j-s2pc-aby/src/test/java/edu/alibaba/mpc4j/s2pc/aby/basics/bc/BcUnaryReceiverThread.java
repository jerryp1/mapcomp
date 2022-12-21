package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

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
    private final BcParty bcSender;
    /**
     * operator
     */
    private final BcOperator booleanOperator;
    /**
     * x bit vector
     */
    private final BitVector xBitVector;
    /**
     * number of bits
     */
    private final int bitNum;
    /**
     * z (plain)
     */
    private BitVector z1;
    /**
     * z (secret)
     */
    private BitVector z0;

    BcUnaryReceiverThread(BcParty bcSender, BcOperator booleanOperator, BitVector xBitVector) {
        this.bcSender = bcSender;
        this.booleanOperator = booleanOperator;
        this.xBitVector = xBitVector;
        bitNum = xBitVector.bitNum();
    }

    BitVector getZ1() {
        return z1;
    }

    BitVector getZ0() {
        return z0;
    }

    @Override
    public void run() {
        try {
            bcSender.getRpc().connect();
            bcSender.init(bitNum, bitNum);
            // set inputs
            SquareSbitVector x = SquareSbitVector.create(xBitVector, true);
            SquareSbitVector x1 = bcSender.shareOther(bitNum);
            //noinspection SwitchStatementWithTooFewBranches
            switch (booleanOperator) {
                case NOT:
                    // (plain, plain)
                    SquareSbitVector z01 = bcSender.not(x);
                    bcSender.revealOther(z01);
                    z0 = bcSender.revealOwn(z01);
                    // (plain, secret)
                    SquareSbitVector z11 = bcSender.not(x1);
                    bcSender.revealOther(z11);
                    z1 = bcSender.revealOwn(z11);
                    break;
                default:
                    throw new IllegalStateException("Invalid unary boolean operator: " + booleanOperator.name());
            }
            bcSender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
