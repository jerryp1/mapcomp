package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.circuit.z2.MpcBcParty;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;

import java.util.Arrays;

/**
 * Boolean circuit party.
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public interface BcParty extends TwoPartyPto, MpcBcParty {
    /**
     * Share its own BitVector.
     *
     * @param xi the BitVector to be shared.
     * @return the shared BitVector.
     */
    @Override
    SquareZ2Vector shareOwn(BitVector xi);

    /**
     * Share its own BitVectors。
     *
     * @param xiArray the BitVectors to be shared.
     * @return the shared BitVectors.
     */
    @Override
    default SquareZ2Vector[] shareOwn(BitVector[] xiArray) {
        if (xiArray.length == 0) {
            return new SquareZ2Vector[0];
        }
        // merge
        BitVector mergeX = BitVectorFactory.merge(xiArray);
        // share
        SquareZ2Vector mergeShareXi = shareOwn(mergeX);
        // split
        int[] bitNums = Arrays.stream(xiArray).mapToInt(BitVector::bitNum).toArray();
        return Arrays.stream(split(mergeShareXi, bitNums))
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
    }

    /**
     * Share other's BitVector.
     *
     * @param bitNum the number of bits to be shared.
     * @return the shared BitVector.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector shareOther(int bitNum) throws MpcAbortException;

    /**
     * Share other's BitVectors.
     *
     * @param bitNums the number of bits for each bit vector to be shared.
     * @return the shared BitVectors.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    default SquareZ2Vector[] shareOther(int[] bitNums) throws MpcAbortException {
        if (bitNums.length == 0) {
            return new SquareZ2Vector[0];
        }
        // share
        int bitNum = Arrays.stream(bitNums).sum();
        SquareZ2Vector mergeShareXi = shareOther(bitNum);
        // split
        return Arrays.stream(split(mergeShareXi, bitNums))
            .map(vector -> (SquareZ2Vector) vector)
            .toArray(SquareZ2Vector[]::new);
    }

    /**
     * Reveal its own BitVectors.
     *
     * @param xiArray the shared BitVectors.
     * @return the reconstructed BitVectors.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    default BitVector[] revealOwn(MpcZ2Vector[] xiArray) throws MpcAbortException {
        if (xiArray.length == 0) {
            return new BitVector[0];
        }
        // merge
        SquareZ2Vector mergeXiArray = (SquareZ2Vector) merge(xiArray);
        // reveal
        BitVector mergeX = revealOwn(mergeXiArray);
        // split
        int[] bitNums = Arrays.stream(xiArray)
            .map(vector -> (SquareZ2Vector) vector)
            .mapToInt(SquareZ2Vector::getNum).toArray();
        return BitVectorFactory.split(mergeX, bitNums);
    }

    /**
     * Reconstruct other's BitVectors.
     *
     * @param xiArray the shared BitVectors.
     */
    @Override
    default void revealOther(MpcZ2Vector[] xiArray) {
        //noinspection StatementWithEmptyBody
        if (xiArray.length == 0) {
            // do nothing for 0 length
        }
        // merge
        SquareZ2Vector mergeXiArray = (SquareZ2Vector) merge(xiArray);
        // reveal
        revealOther(mergeXiArray);
    }

    /**
     * AND operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x & y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector and(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector AND operations.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] & y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector[] and(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * XOR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x ^ y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector xor(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException;

    /**
     * Vector XOR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] ^ y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector[] xor(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException;

    /**
     * NOT operation.
     *
     * @param xi xi.
     * @return zi, such that z = !x.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector not(MpcZ2Vector xi) throws MpcAbortException;

    /**
     * Vector NOT operation.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = !x[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZ2Vector[] not(MpcZ2Vector[] xiArray) throws MpcAbortException;

    /**
     * OR operation.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x | y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    default SquareZ2Vector or(MpcZ2Vector xi, MpcZ2Vector yi) throws MpcAbortException {
        return xor(xor(xi, yi), and(xi, yi));
    }

    /**
     * Vector OR operation.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = z[i] | y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    default SquareZ2Vector[] or(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        return xor(xor(xiArray, yiArray), and(xiArray, yiArray));
    }
}
