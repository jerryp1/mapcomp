package edu.alibaba.mpc4j.crypto.matrix;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import org.apache.commons.lang3.time.StopWatch;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Transpose utilities.
 *
 * @author Li Peng
 * @date 2023/11/7
 */
public class TransposeUtils {

    public static long TRANSPORT_TIME = 0;
    public static StopWatch STOPWATCH = new StopWatch();
    /**
     * Transpose bitvectors to vector of byte arrays.
     *
     * @param input bitvector
     * @return vectors of byte arrays.
     */
    public static Vector<byte[]> transposeMergeToVector(BitVector[] input) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ZlDatabase zlDatabase = ZlDatabase.create(EnvType.STANDARD_JDK, true, input);
        Vector<byte[]> result = Arrays.stream(zlDatabase.getBytesData()).collect(Collectors.toCollection(Vector::new));
        stopWatch.stop();
        TRANSPORT_TIME += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        return result;
    }

    /**
     * Transpose bitvectors to array of byte arrays.
     *
     * @param input bitvector.
     * @return arrays of byte arrays.
     */
    public static byte[][] transposeMerge(BitVector[] input) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ZlDatabase zlDatabase = ZlDatabase.create(EnvType.STANDARD_JDK, true, input);
        byte[][] result = zlDatabase.getBytesData();
        stopWatch.stop();
        TRANSPORT_TIME += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        return result;
    }

    /**
     * Transpose vectors of byte array to bitvectors.
     *
     * @param input vector of byte arrays.
     * @param l     bit length.
     * @return byte arrays to bitvectors.
     */
    public static BitVector[] transposeSplit(Vector<byte[]> input, int l) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        BitVector[] result = transposeSplit(input.toArray(new byte[0][]), l);
        stopWatch.stop();
        TRANSPORT_TIME += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        return result;
    }

    /**
     * Transpose arrays of byte arrays to bitvectors.
     *
     * @param input vector of byte array
     * @param l     bit length.
     * @return byte arrays to bitvectors.
     */
    public static BitVector[] transposeSplit(byte[][] input, int l) {
        StopWatch stopWatch = new StopWatch();
        ZlDatabase zlDatabase = ZlDatabase.create(l, input);
        stopWatch.start();
        BitVector[] result = Arrays.stream(zlDatabase.bitPartition(EnvType.STANDARD, true))
            .toArray(BitVector[]::new);
        stopWatch.stop();
        TRANSPORT_TIME += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        return result;
    }

    /**
     * Transpose arrays of byte arrays to bitvectors.
     *
     * @param input vector of byte array
     * @param l     bit length.
     * @return byte arrays to bitvectors.
     */
    public static BitVector[] transposeSplit(BigInteger[] input, int l) {
        StopWatch stopWatch = new StopWatch();
        ZlDatabase zlDatabase = ZlDatabase.create(l, input);
        stopWatch.start();
        BitVector[] result = Arrays.stream(zlDatabase.bitPartition(EnvType.STANDARD, true))
            .toArray(BitVector[]::new);
        stopWatch.stop();
        TRANSPORT_TIME += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        return result;
    }
}
