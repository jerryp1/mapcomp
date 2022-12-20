package edu.alibaba.mpc4j.s2pc.aby.bc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Sender test thread for Boolean circuit unary operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class BcUnarySenderThread extends Thread {
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
     * z bit vector
     */
    private final BitVector zBitVector;
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

    BcUnarySenderThread(BcParty bcSender, BcOperator bcOperator, BitVector xBitVector) {
        this.bcSender = bcSender;
        this.booleanOperator = bcOperator;
        this.xBitVector = xBitVector;
        bitNum = xBitVector.bitNum();
        //noinspection SwitchStatementWithTooFewBranches
        switch (bcOperator) {
            case NOT:
                zBitVector = xBitVector.not();
                break;
            default:
                throw new IllegalStateException("Invalid unary boolean operator: " + booleanOperator.name());
        }
    }

    BitVector getZ() {
        return zBitVector;
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
            SquareSbitVector x0 = bcSender.shareOwn(xBitVector);
            SquareSbitVector z00, z10;
            //noinspection SwitchStatementWithTooFewBranches
            switch (booleanOperator) {
                case NOT:
                    // (plain, plain)
                    z00 = bcSender.not(x);
                    z0 = bcSender.revealOwn(z00);
                    bcSender.revealOther(z00);
                    // (plain, secret)
                    z10 = bcSender.not(x0);
                    z1 = bcSender.revealOwn(z10);
                    bcSender.revealOther(z10);
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
