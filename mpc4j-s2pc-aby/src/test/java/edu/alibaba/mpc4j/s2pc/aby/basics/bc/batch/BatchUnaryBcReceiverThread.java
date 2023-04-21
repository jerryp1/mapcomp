package edu.alibaba.mpc4j.s2pc.aby.basics.bc.batch;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

import java.util.Arrays;

/**
 * batch Boolean circuit receiver thread for unary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BatchUnaryBcReceiverThread extends Thread {
    /**
     * sender
     */
    private final BcParty receiver;
    /**
     * operator
     */
    private final BcOperator bcOperator;
    /**
     * x bit vectors
     */
    private final BitVector[] xBitVectors;
    /**
     * total number of bits
     */
    private final int totalBitNum;
    /**
     * share x1 array
     */
    private SquareShareZ2Vector[] shareX1s;
    /**
     * final x1 array
     */
    private SquareShareZ2Vector[] finalX1s;
    /**
     * z (plain)
     */
    private BitVector[] z1Vectors;
    /**
     * z (secret)
     */
    private BitVector[] z0Vectors;

    BatchUnaryBcReceiverThread(BcParty receiver, BcOperator bcOperator, BitVector[] xBitVectors) {
        this.receiver = receiver;
        this.bcOperator = bcOperator;
        this.xBitVectors = xBitVectors;
        totalBitNum = Arrays.stream(xBitVectors).mapToInt(BitVector::bitNum).sum();
    }

    BitVector[] getZ1Vectors() {
        return z1Vectors;
    }

    BitVector[] getZ0Vectors() {
        return z0Vectors;
    }

    SquareShareZ2Vector[] getShareX1s() {
        return shareX1s;
    }

    SquareShareZ2Vector[] getFinalX1s() {
        return finalX1s;
    }

    @Override
    public void run() {
        try {
            receiver.init(totalBitNum, totalBitNum);
            // set inputs
            SquareShareZ2Vector[] xs = Arrays.stream(xBitVectors)
                .map(xBitVector -> SquareShareZ2Vector.create(xBitVector, true))
                .toArray(SquareShareZ2Vector[]::new);
            int[] xLengths = Arrays.stream(xBitVectors).mapToInt(BitVector::bitNum).toArray();
            SquareShareZ2Vector[] x1s = receiver.shareOther(xLengths);
            shareX1s = Arrays.stream(x1s).map(SquareShareZ2Vector::copy).toArray(SquareShareZ2Vector[]::new);
            SquareShareZ2Vector[] z01s, z11s;
            //noinspection SwitchStatementWithTooFewBranches
            switch (bcOperator) {
                case NOT:
                    // (plain, plain)
                    z01s = receiver.not(xs);
                    receiver.revealOther(z01s);
                    z0Vectors = receiver.revealOwn(z01s);
                    // (plain, secret)
                    z11s = receiver.not(x1s);
                    finalX1s = Arrays.stream(x1s).map(SquareShareZ2Vector::copy).toArray(SquareShareZ2Vector[]::new);
                    receiver.revealOther(z11s);
                    z1Vectors = receiver.revealOwn(z11s);
                    break;
                default:
                    throw new IllegalStateException("Invalid unary boolean operator: " + bcOperator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
