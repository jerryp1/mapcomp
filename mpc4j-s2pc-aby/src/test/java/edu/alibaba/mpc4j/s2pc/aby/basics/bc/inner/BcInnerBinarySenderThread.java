package edu.alibaba.mpc4j.s2pc.aby.basics.bc.inner;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

/**
 * Sender test thread for Boolean circuit inner binary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BcInnerBinarySenderThread extends Thread {
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

    BcInnerBinarySenderThread(BcParty bcSender, BcOperator bcOperator, BitVector xBitVector, BitVector yBitVector) {
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
            SquareSbitVector xCopy = SquareSbitVector.createCopy(xBitVector, true);
            SquareSbitVector x;
            SquareSbitVector y = SquareSbitVector.createCopy(yBitVector, true);
            SquareSbitVector x0Copy = bcSender.shareOwn(xBitVector);
            SquareSbitVector x0;
            SquareSbitVector y0 = bcSender.shareOther(bitNum);
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    x = xCopy.copy();
                    bcSender.xori(x, y);
                    z11 = bcSender.revealOwn(x);
                    bcSender.revealOther(x);
                    // (plain, secret)
                    x = xCopy.copy();
                    bcSender.xori(x, y0);
                    z10 = bcSender.revealOwn(x);
                    bcSender.revealOther(x);
                    // (secret, plain)
                    x0 = x0Copy.copy();
                    bcSender.xori(x0, y);
                    z01 = bcSender.revealOwn(x0);
                    bcSender.revealOther(x0);
                    // (secret, secret)
                    x0 = x0Copy.copy();
                    bcSender.xori(x0, y0);
                    z00 = bcSender.revealOwn(x0);
                    bcSender.revealOther(x0);
                    break;
                case AND:
                    // (plain, plain)
                    x = xCopy.copy();
                    bcSender.andi(x, y);
                    z11 = bcSender.revealOwn(x);
                    bcSender.revealOther(x);
                    // (plain, secret)
                    x = xCopy.copy();
                    bcSender.andi(x, y0);
                    z10 = bcSender.revealOwn(x);
                    bcSender.revealOther(x);
                    // (secret, plain)
                    x0 = x0Copy.copy();
                    bcSender.andi(x0, y);
                    z01 = bcSender.revealOwn(x0);
                    bcSender.revealOther(x0);
                    // (secret, secret)
                    x0 = x0Copy.copy();
                    bcSender.andi(x0, y0);
                    z00 = bcSender.revealOwn(x0);
                    bcSender.revealOther(x0);
                    break;
                case OR:
                    // (plain, plain)
                    x = xCopy.copy();
                    bcSender.ori(x, y);
                    z11 = bcSender.revealOwn(x);
                    bcSender.revealOther(x);
                    // (plain, secret)
                    x = xCopy.copy();
                    bcSender.ori(x, y0);
                    z10 = bcSender.revealOwn(x);
                    bcSender.revealOther(x);
                    // (secret, plain)
                    x0 = x0Copy.copy();
                    bcSender.ori(x0, y);
                    z01 = bcSender.revealOwn(x0);
                    bcSender.revealOther(x0);
                    // (secret, secret)
                    x0 = x0Copy.copy();
                    bcSender.ori(x0, y0);
                    z00 = bcSender.revealOwn(x0);
                    bcSender.revealOther(x0);
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
