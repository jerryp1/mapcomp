package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.zl.MpcZlParty;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;

import java.util.Arrays;

/**
 * Zl circuit party.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public interface ZlcParty extends TwoPartyPto, MpcZlParty {
    /**
     * Shares public vector.
     *
     * @param xi public vector to be shared.
     * @return the shared vector.
     */
    @Override
    SquareZlVector setPublicValue(ZlVector xi);

    /**
     * Shares its own vector.
     *
     * @param xi the vector to be shared.
     * @return the shared vector.
     */
    @Override
    default SquareZlVector shareOwn(ZlVector xi){
        return SquareZlVector.create(xi, false);
    }

    /**
     * Shares its own vectors。
     *
     * @param xiArray the vectors to be shared.
     * @return the shared vectors.
     */
    @Override
    default SquareZlVector[] shareOwn(ZlVector[] xiArray) {
        if (xiArray.length == 0) {
            return new SquareZlVector[0];
        }
        return Arrays.stream(xiArray).map(x -> SquareZlVector.create(x, false)).toArray(SquareZlVector[]::new);
    }

    /**
     * Shares other's vector.
     *
     * @param num the num to be shared.
     * @return the shared vector.
     */
    @Override
    default SquareZlVector shareOther(int num) {
        return SquareZlVector.create(ZlVector.createZeros(getZl(), num), false);
    }

    /**
     * Shares other's vectors.
     *
     * @param nums nums for each vector to be shared.
     * @return the shared vectors.
     */
    @Override
    default SquareZlVector[] shareOther(int[] nums){
        if (nums.length == 0) {
            return new SquareZlVector[0];
        }
        return Arrays.stream(nums).mapToObj(n ->
            SquareZlVector.create(ZlVector.createZeros(getZl(), n), false)).toArray(SquareZlVector[]::new);
    }

    /**
     * Reveals its own vectors.
     *
     * @param xiArray the shared vectors.
     * @return the revealed vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    @Override
    default ZlVector[] revealOwn(MpcZlVector[] xiArray) throws MpcAbortException {
        if (xiArray.length == 0) {
            return new ZlVector[0];
        }
        // merge
        SquareZlVector mergeXiArray = (SquareZlVector) merge(xiArray);
        // reveal
        ZlVector mergeX = revealOwn(mergeXiArray);
        // split
        int[] nums = Arrays.stream(xiArray).parallel()
            .map(vector -> (SquareZlVector) vector)
            .mapToInt(SquareZlVector::getNum).toArray();
        return ZlVector.split(mergeX, nums);
    }

    /**
     * Reveals other's vectors.
     *
     * @param xiArray the shared vectors.
     */
    @Override
    default void revealOther(MpcZlVector[] xiArray) {
        //noinspection StatementWithEmptyBody
        if (xiArray.length == 0) {
            // do nothing for 0 length
        }
        // merge
        SquareZlVector mergeXiArray = (SquareZlVector) merge(xiArray);
        // reveal
        revealOther(mergeXiArray);
    }

    /**
     * Addition.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x + y.
     */
    @Override
    SquareZlVector add(MpcZlVector xi, MpcZlVector yi);

    /**
     * Vector addition.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] + y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector[] add(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException;

    /**
     * Subtraction.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x - y.
     */
    @Override
    SquareZlVector sub(MpcZlVector xi, MpcZlVector yi);

    /**
     * Vector subtraction.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = x[i] - y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector[] sub(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException;

    /**
     * Negation.
     *
     * @param xi xi.
     * @return zi, such that z = -x.
     */
    @Override
    SquareZlVector neg(MpcZlVector xi);

    /**
     * Vector negation.
     *
     * @param xiArray xi array.
     * @return zi array, such that for each j, z[i] = -x[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector[] neg(MpcZlVector[] xiArray) throws MpcAbortException;

    /**
     * Multiplication.
     *
     * @param xi xi.
     * @param yi yi.
     * @return zi, such that z = x * y.
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector mul(MpcZlVector xi, MpcZlVector yi) throws MpcAbortException;

    /**
     * Vector multiplication.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, such that for each j, z[i] = z[i] * y[i].
     * @throws MpcAbortException if the protocol is abort.
     */
    @Override
    SquareZlVector[] mul(MpcZlVector[] xiArray, MpcZlVector[] yiArray) throws MpcAbortException;

    /**
     * compute the prefix sum of vector
     *
     * @param x    the element vector
     * @param prefix the prefix value
     * @return a new vector: y[i] = \sum_0^i{x[i]} + prefix
     */
    SquareZlVector rowAdderWithPrefix(SquareZlVector x, SquareZlVector prefix);
}
