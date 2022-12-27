package edu.alibaba.mpc4j.s2pc.aby.basics.bc.inner;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

/**
 * Sender test thread for Boolean circuit inner unary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BcInnerUnarySenderThread extends Thread {
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

    BcInnerUnarySenderThread(BcParty bcSender, BcOperator bcOperator, BitVector xBitVector) {
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
            SquareSbitVector xCopy = SquareSbitVector.createCopy(xBitVector, true);
            SquareSbitVector x;
            SquareSbitVector x0Copy = bcSender.shareOwn(xBitVector);
            SquareSbitVector x0;
            //noinspection SwitchStatementWithTooFewBranches
            switch (booleanOperator) {
                case NOT:
                    // (plain, plain)
                    x = xCopy.copy();
                    bcSender.noti(x);
                    z0 = bcSender.revealOwn(x);
                    bcSender.revealOther(x);
                    // (plain, secret)
                    x0 = x0Copy.copy();
                    bcSender.noti(x0);
                    z1 = bcSender.revealOwn(x0);
                    bcSender.revealOther(x0);
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
