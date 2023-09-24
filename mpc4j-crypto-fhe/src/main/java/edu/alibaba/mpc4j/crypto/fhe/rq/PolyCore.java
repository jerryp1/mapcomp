package edu.alibaba.mpc4j.crypto.fhe.rq;

import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/8/29
 */
public class PolyCore {

    /**
     * @param coeffCount       N
     * @param coeffUint64Count k
     * @return an array with length K * N
     */
    public static long[] allocatePoly(int coeffCount, int coeffUint64Count) {

        return new long[Common.mulSafe(coeffUint64Count, coeffCount, false)];
    }

    public static void setZeroPoly(int coeffCount, int coeffUint64Count, long[] poly) {
        assert coeffCount > 0 && coeffUint64Count > 0;

        UintCore.setZeroUint(Common.mulSafe(coeffCount, coeffUint64Count, false), poly);
    }

    public static void setZeroPolyArray(int polyCount, int coeffCount, int coeffUint64Count, long[] result) {

        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;

        UintCore.setZeroUint(Common.mulSafe(polyCount, coeffCount, false, coeffUint64Count), result);
    }


    public static long[] allocateZeroPoly(int coeffCount, int coeffUint64Count) {

        assert coeffCount > 0 && coeffUint64Count > 0;

        return new long[Common.mulSafe(coeffCount, coeffUint64Count, false)];
    }


    public static long[] allocatePolyArray(int polyCount, int coeffCount, int coeffUint64Count) {
        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;

        return new long[Common.mulSafe(polyCount, coeffCount, false, coeffUint64Count)];
    }

    public static long[] allocateZeroPolyArray(int polyCount, int coeffCount, int coeffUint64Count) {
        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;

        return new long[Common.mulSafe(polyCount, coeffCount, false, coeffUint64Count)];
    }

    public static void setPoly(long[] poly, int coeffCount, int coeffUint64Count, long[] result) {

        assert coeffCount > 0 && coeffUint64Count > 0;

        UintCore.setUint(poly, Common.mulSafe(coeffCount, coeffUint64Count, false), result);

    }

    public static void setPolyArray(long[] poly, int polyCount, int coeffCount, int coeffUint64Count, long[] result) {

        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;


        UintCore.setUint(poly, Common.mulSafe(coeffCount, coeffUint64Count, false, polyCount), result);
    }


}
