package edu.alibaba.mpc4j.crypto.fhe.rq;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/8/29
 */
public class PolyCore {


    /**
     *
     * @param coeffCount N
     * @param coeffUint64Count k
     * @return a matrix with shape (coeffUint64Count, coeffCount)
     */
    public static long[][] allocatePoly(int coeffCount, int coeffUint64Count) {

        assert coeffCount > 0 && coeffUint64Count > 0;

        return new long[coeffUint64Count][coeffCount];
    }

    public static void setZeroPoly(int coeffCount, int coeffUint64Count, long[][] poly) {
        assert coeffCount > 0 && coeffUint64Count > 0;

        IntStream.range(0, coeffUint64Count).parallel().forEach(
                i -> Arrays.fill(poly[i], 0)
        );
    }

    public static void setZeroPolyArray(int polyCount, int coeffCount, int coeffUint64Count, long[][][] result) {

        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;

        IntStream.range(0, polyCount).parallel().forEach(
                i -> setZeroPoly(coeffCount, coeffUint64Count, result[i])
        );

    }



    public static long[][] allocateZeroPoly(int coeffCount, int coeffUint64Count) {

        assert coeffCount > 0 && coeffUint64Count > 0;

        return new long[coeffUint64Count][coeffCount];
    }


    public static long[][][] allocatePolyArray(int polyCount, int coeffCount, int coeffUint64Count) {
        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;

        return new long[polyCount][coeffUint64Count][coeffCount];
    }

    public static long[][][] allocateZeroPolyArray(int polyCount, int coeffCount, int coeffUint64Count) {
        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;

        return new long[polyCount][coeffUint64Count][coeffCount];
    }

    public static void setPoly(long[][] poly, int coeffCount, int coeffUint64Count, long[][] result) {

        assert coeffCount > 0 && coeffUint64Count > 0;
        assert poly[0].length == coeffCount;
        assert poly[0].length == result[0].length;


        IntStream.range(0, coeffUint64Count).parallel().forEach(
                i -> System.arraycopy(poly[i], 0, result[i], 0, coeffCount)
        );

    }

    public static void setPolyArray(long[][][] poly, int polyCount, int coeffCount, int coeffUint64Count, long[][][] result) {

        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;


        IntStream.range(0, polyCount).parallel().forEach(
                i -> setPoly(poly[i], coeffCount, coeffUint64Count, result[i])
        );

    }




}
