package edu.alibaba.mpc4j.s2pc.aby.basics.bc.vector;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.matrix.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Sender test thread for vector Boolean circuit binary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BcVectorBinarySenderThread extends Thread {
    /**
     * sender
     */
    private final BcParty sender;
    /**
     * operator
     */
    private final BcOperator bcOperator;
    /**
     * x bit vectors
     */
    private final BitVector[] xBitVectors;
    /**
     * y bit vectors
     */
    private final BitVector[] yBitVectors;
    /**
     * expect bit vector
     */
    private final BitVector[] expectBitVectors;
    /**
     * total number of bits
     */
    private final int totalBitNum;
    /**
     * share x0 array
     */
    private SquareSbitVector[] shareX0s;
    /**
     * final x100 array
     */
    private SquareSbitVector[] finalX010s;
    /**
     * final x000 array
     */
    private SquareSbitVector[] finalX000s;
    /**
     * z array (plain, plain)
     */
    private BitVector[] z11Vectors;
    /**
     * z array (plain, secret)
     */
    private BitVector[] z10Vectors;
    /**
     * z array (secret, plain)
     */
    private BitVector[] z01Vectors;
    /**
     * z array (secret, secret)
     */
    private BitVector[] z00Vectors;

    BcVectorBinarySenderThread(BcParty sender, BcOperator bcOperator,
                               BitVector[] xBitVectors, BitVector[] yBitVectors) {
        this.sender = sender;
        this.bcOperator = bcOperator;
        this.xBitVectors = xBitVectors;
        this.yBitVectors = yBitVectors;
        totalBitNum = Arrays.stream(xBitVectors).mapToInt(BitVector::bitNum).sum();
        int vectorLength = xBitVectors.length;
        switch (bcOperator) {
            case XOR:
                expectBitVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xBitVectors[index].xor(yBitVectors[index]))
                    .toArray(BitVector[]::new);
                break;
            case AND:
                expectBitVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xBitVectors[index].and(yBitVectors[index]))
                    .toArray(BitVector[]::new);
                break;
            case OR:
                expectBitVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xBitVectors[index].or(yBitVectors[index]))
                    .toArray(BitVector[]::new);
                break;
            default:
                throw new IllegalStateException("Invalid binary boolean operator: " + bcOperator.name());
        }
    }

    BitVector[] getExpectVectors() {
        return expectBitVectors;
    }

    BitVector[] getZ11Vectors() {
        return z11Vectors;
    }

    BitVector[] getZ10Vectors() {
        return z10Vectors;
    }

    BitVector[] getZ01Vectors() {
        return z01Vectors;
    }

    BitVector[] getZ00Vectors() {
        return z00Vectors;
    }

    SquareSbitVector[] getShareX0s() {
        return shareX0s;
    }

    SquareSbitVector[] getFinalX010s() {
        return finalX010s;
    }

    SquareSbitVector[] getFinalX000s() {
        return finalX000s;
    }

    @Override
    public void run() {
        try {
            sender.init(totalBitNum, totalBitNum);
            // set inputs
            SquareSbitVector[] xs = Arrays.stream(xBitVectors)
                .map(xBitVector -> SquareSbitVector.create(xBitVector, true))
                .toArray(SquareSbitVector[]::new);
            SquareSbitVector[] ys = Arrays.stream(yBitVectors)
                .map(yBitVector -> SquareSbitVector.create(yBitVector, true))
                .toArray(SquareSbitVector[]::new);
            SquareSbitVector[] x0s = sender.shareOwn(xBitVectors);
            shareX0s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
            int[] vectorBitLengths = Arrays.stream(yBitVectors).mapToInt(BitVector::bitNum).toArray();
            SquareSbitVector[] y0s = sender.shareOther(vectorBitLengths);
            SquareSbitVector[] z110s, z100s, z010s, z000s;
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    z110s = sender.xor(xs, ys);
                    z11Vectors = sender.revealOwn(z110s);
                    sender.revealOther(z110s);
                    // (plain, secret)
                    z100s = sender.xor(xs, y0s);
                    z10Vectors = sender.revealOwn(z100s);
                    sender.revealOther(z100s);
                    // (secret, plain)
                    z010s = sender.xor(x0s, ys);
                    finalX010s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z01Vectors = sender.revealOwn(z010s);
                    sender.revealOther(z010s);
                    // (secret, secret)
                    z000s = sender.xor(x0s, y0s);
                    finalX000s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z00Vectors = sender.revealOwn(z000s);
                    sender.revealOther(z000s);
                    break;
                case AND:
                    // (plain, plain)
                    z110s = sender.and(xs, ys);
                    z11Vectors = sender.revealOwn(z110s);
                    sender.revealOther(z110s);
                    // (plain, secret)
                    z100s = sender.and(xs, y0s);
                    z10Vectors = sender.revealOwn(z100s);
                    sender.revealOther(z100s);
                    // (secret, plain)
                    z010s = sender.and(x0s, ys);
                    finalX010s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z01Vectors = sender.revealOwn(z010s);
                    sender.revealOther(z010s);
                    // (secret, secret)
                    z000s = sender.and(x0s, y0s);
                    finalX000s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z00Vectors = sender.revealOwn(z000s);
                    sender.revealOther(z000s);
                    break;
                case OR:
                    // (plain, plain)
                    z110s = sender.or(xs, ys);
                    z11Vectors = sender.revealOwn(z110s);
                    sender.revealOther(z110s);
                    // (plain, secret)
                    z100s = sender.or(xs, y0s);
                    z10Vectors = sender.revealOwn(z100s);
                    sender.revealOther(z100s);
                    // (secret, plain)
                    z010s = sender.or(x0s, ys);
                    finalX010s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z01Vectors = sender.revealOwn(z010s);
                    sender.revealOther(z010s);
                    // (secret, secret)
                    z000s = sender.or(x0s, y0s);
                    finalX000s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z00Vectors = sender.revealOwn(z000s);
                    sender.revealOther(z000s);
                    break;
                default:
                    throw new IllegalStateException("Invalid binary boolean operator: " + bcOperator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
