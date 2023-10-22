package edu.alibaba.mpc4j.crypto.fhe.rq;

import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;


/**
 * This class provides some helper methods for polynomials.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/polycore.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/8/29
 */
public class PolyCore {


    /**
     * Convert a poly (N coefficients, each coefficient is a base-2^64 value with length l) to hexString
     *
     * @param value            表示一个多项式的系数，总共 N 个系数，每一个都是一个 uint，长度为 coeffUint64Count
     * @param coeffCount       N
     * @param coeffUint64Count 每一个系数的uint64长度, l
     * @return hex string of the value(poly)
     */
    public static String polyToHexString(long[] value, int coeffCount, int coeffUint64Count) {

        assert value != null;

        if (coeffCount == 0 || coeffUint64Count == 0) {
            return "0";
        }

        StringBuilder result = new StringBuilder();
        boolean empty = true;

        int valueIndex = Common.mulSafe(coeffCount - 1, coeffUint64Count, false);
        // 依次处理每一个coeff, 即 [(N-1) * l , N * l) --> [(N-2) * l, (N-1) * l) ---> .... ---> [0, 1* l)
        while (coeffCount-- > 0) {
            // 如果最后一个 coeff 为0
            if (UintCore.isZeroUint(value, valueIndex, coeffUint64Count)) {
                valueIndex -= coeffUint64Count;
                continue;
            }
            if (!empty) {
                result.append(" + ");
            }
            result.append(UintCore.uintToHexString(value, valueIndex, coeffUint64Count));
            if (coeffCount > 0) {
                result.append("x^");
                result.append(coeffCount);
            }
            empty = false;
            valueIndex -= coeffUint64Count;
        }
        if (empty) {
            result.append('0');
        }
        return result.toString();
    }


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
        assert poly != null;
        assert result != null;
        assert coeffCount > 0 && coeffUint64Count > 0;

        UintCore.setUint(poly, Common.mulSafe(coeffCount, coeffUint64Count, false), result);

    }

    public static void setPolyArray(long[] poly, int polyCount, int coeffCount, int coeffUint64Count, long[] result) {

        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;


        UintCore.setUint(poly, Common.mulSafe(coeffCount, coeffUint64Count, false, polyCount), result);
    }


}
