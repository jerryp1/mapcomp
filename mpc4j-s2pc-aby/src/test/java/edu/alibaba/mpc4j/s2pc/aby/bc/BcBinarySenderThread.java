package edu.alibaba.mpc4j.s2pc.aby.bc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * Sender test thread for Boolean circuit binary operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class BcBinarySenderThread extends Thread {
    /**
     * sender
     */
    private final BcParty bcSender;
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
     * z bit vector
     */
    private final BitVector zBitVector;
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

    BcBinarySenderThread(BcParty bcSender, BcOperator bcOperator, BitVector xBitVector, BitVector yBitVector) {
        this.bcSender = bcSender;
        this.bcOperator = bcOperator;
        assert xBitVector.bitNum() == yBitVector.bitNum();
        this.xBitVector = xBitVector;
        this.yBitVector = yBitVector;
        bitNum = xBitVector.bitNum();
        switch (bcOperator) {
            case XOR:
                zBitVector = xBitVector.xor(yBitVector);
                break;
            case AND:
                zBitVector = xBitVector.and(yBitVector);
                break;
            case OR:
                zBitVector = xBitVector.or(yBitVector);
                break;
            default:
                throw new IllegalStateException("Invalid binary boolean operator: " + bcOperator.name());
        }
    }

    BitVector getZ() {
        return zBitVector;
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
            bcSender.getRpc().connect();
            bcSender.init(bitNum, bitNum);
            // set inputs
            SquareSbitVector x = SquareSbitVector.create(xBitVector, true);
            SquareSbitVector y = SquareSbitVector.create(yBitVector, true);
            SquareSbitVector x0 = bcSender.shareOwn(xBitVector);
            SquareSbitVector y0 = bcSender.shareOther(bitNum);
            SquareSbitVector z110, z100, z010, z000;
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    z110 = bcSender.xor(x, y);
                    z11 = bcSender.revealOwn(z110);
                    bcSender.revealOther(z110);
                    // (plain, secret)
                    z100 = bcSender.xor(x, y0);
                    z10 = bcSender.revealOwn(z100);
                    bcSender.revealOther(z100);
                    // (secret, plain)
                    z010 = bcSender.xor(x0, y);
                    z01 = bcSender.revealOwn(z010);
                    bcSender.revealOther(z010);
                    // (secret, secret)
                    z000 = bcSender.xor(x0, y0);
                    z00 = bcSender.revealOwn(z000);
                    bcSender.revealOther(z000);
                    break;
                case AND:
                    // (plain, plain)
                    z110 = bcSender.and(x, y);
                    z11 = bcSender.revealOwn(z110);
                    bcSender.revealOther(z110);
                    // (plain, secret)
                    z100 = bcSender.and(x, y0);
                    z10 = bcSender.revealOwn(z100);
                    bcSender.revealOther(z100);
                    // (secret, plain)
                    z010 = bcSender.and(x0, y);
                    z01 = bcSender.revealOwn(z010);
                    bcSender.revealOther(z010);
                    // (secret, secret)
                    z000 = bcSender.and(x0, y0);
                    z00 = bcSender.revealOwn(z000);
                    bcSender.revealOther(z000);
                    break;
                case OR:
                    // (plain, plain)
                    z110 = bcSender.or(x, y);
                    z11 = bcSender.revealOwn(z110);
                    bcSender.revealOther(z110);
                    // (plain, secret)
                    z100 = bcSender.or(x, y0);
                    z10 = bcSender.revealOwn(z100);
                    bcSender.revealOther(z100);
                    // (secret, plain)
                    z010 = bcSender.or(x0, y);
                    z01 = bcSender.revealOwn(z010);
                    bcSender.revealOther(z010);
                    // (secret, secret)
                    z000 = bcSender.or(x0, y0);
                    z00 = bcSender.revealOwn(z000);
                    bcSender.revealOther(z000);
                    break;
                default:
                    throw new IllegalStateException("Invalid binary boolean operator: " + bcOperator.name());
            }
            bcSender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
