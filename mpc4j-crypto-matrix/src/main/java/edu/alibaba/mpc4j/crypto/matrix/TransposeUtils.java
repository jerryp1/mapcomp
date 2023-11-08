package edu.alibaba.mpc4j.crypto.matrix;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;

import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Transpose utilities.
 *
 * @author Li Peng
 * @date 2023/11/7
 */
public class TransposeUtils {
    /**
     * Transpose bitvectors to vector of byte arrays.
     *
     * @param input bitvector
     * @return vectors of byte arrays.
     */
    public static Vector<byte[]> transposeMergeToVector(BitVector[] input) {
        ZlDatabase zlDatabase = ZlDatabase.create(EnvType.STANDARD_JDK, true, input);
        return Arrays.stream(zlDatabase.getBytesData()).collect(Collectors.toCollection(Vector::new));
    }

    /**
     * Transpose bitvectors to array of byte arrays.
     *
     * @param input bitvector.
     * @return arrays of byte arrays.
     */
    public static byte[][] transposeMerge(BitVector[] input) {
        ZlDatabase zlDatabase = ZlDatabase.create(EnvType.STANDARD_JDK, true, input);
        return zlDatabase.getBytesData();
    }

    /**
     * Transpose vectors of byte array to bitvectors.
     *
     * @param input vector of byte arrays.
     * @param l     bit length.
     * @return byte arrays to bitvectors.
     */
    public static BitVector[] transposeSplit(Vector<byte[]> input, int l) {
        return transposeSplit(input.toArray(new byte[0][]), l);
    }

    /**
     * Transpose arrays of byte arrays to bitvectors.
     *
     * @param input vector of byte array
     * @param l     bit length.
     * @return byte arrays to bitvectors.
     */
    public static BitVector[] transposeSplit(byte[][] input, int l) {
        ZlDatabase zlDatabase = ZlDatabase.create(l, input);
        return Arrays.stream(zlDatabase.bitPartition(EnvType.STANDARD, true))
            .toArray(BitVector[]::new);
    }
}
