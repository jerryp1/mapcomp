package edu.alibaba.mpc4j.crypto.fhe.rq;

import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;

/**
 * This class provides some helper methods for polynomials.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/polycore.h
 * </p>
 *
 * @author Qixian Zhou, Weiran Liu
 * @date 2023/8/29
 */
public class PolyCore {
    /**
     * Converts a polynomial (N coefficients, each coefficient is a base-2^64 value with length l) to a hex string.
     *
     * @param value            the polynomial.
     * @param coeffCount       number of coefficients N.
     * @param coeffUint64Count uint64 length l for each coefficient.
     * @return a hex string.
     */
    public static String polyToHexString(long[] value, int coeffCount, int coeffUint64Count) {
        return polyToHexString(value, 0, coeffCount, coeffUint64Count);
    }

    /**
     * Converts a polynomial (N coefficients, each coefficient is a base-2^64 value with length l) to a hex string.
     *
     * @param value            the polynomial.
     * @param pos              the start position.
     * @param coeffCount       number of coefficients N.
     * @param coeffUint64Count uint64 length l for each coefficient.
     * @return a hex string.
     */
    public static String polyToHexString(long[] value, int pos, int coeffCount, int coeffUint64Count) {
        assert value != null;
        // First check if there is anything to print
        if (coeffCount == 0 || coeffUint64Count == 0) {
            return "0";
        }

        StringBuilder result = new StringBuilder();
        boolean empty = true;
        int valueIndex = Common.addSafe(pos, Common.mulSafe(coeffCount - 1, coeffUint64Count, false), false);
        // handle each coefficient
        while (coeffCount-- > 0) {
            // handle if the last coefficient is 0
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
     * Sets the polynomial to zero.
     *
     * @param coeffCount       number of coefficients N.
     * @param coeffUint64Count uint64 length l for each coefficient.
     * @param poly             the polynomial.
     */
    static void setZeroPoly(int coeffCount, int coeffUint64Count, long[] poly) {
        assert coeffCount > 0 && coeffUint64Count > 0;

        UintCore.setZeroUint(Common.mulSafe(coeffCount, coeffUint64Count, false), poly, 0);
    }

    /**
     * Sets the polynomials to zero.
     *
     * @param polyCount        the number of polynomials.
     * @param coeffCount       number of coefficients N for each polynomial.
     * @param coeffUint64Count uint64 length l for each coefficient.
     * @param polyArray        the polynomials.
     */
    static void setZeroPolyArray(int polyCount, int coeffCount, int coeffUint64Count, long[] polyArray) {
        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;

        UintCore.setZeroUint(Common.mulSafe(polyCount, coeffCount, false, coeffUint64Count), polyArray, 0);
    }

    /**
     * Allocates a zero polynomial.
     *
     * @param coeffCount       number of coefficients N.
     * @param coeffUint64Count uint64 length l for each coefficient.
     * @return an array with length l * N that stores the polynomial, all elements are 0.
     */
    static long[] allocateZeroPoly(int coeffCount, int coeffUint64Count) {
        assert coeffCount > 0 && coeffUint64Count > 0;

        return new long[Common.mulSafe(coeffCount, coeffUint64Count, false)];
    }

    /**
     * Allocates a polynomial array with length m.
     *
     * @param polyCount        the number of polynomials m.
     * @param coeffCount       number of coefficients N for each polynomial.
     * @param coeffUint64Count uint64 length l for each coefficient.
     * @return an array with length m * l * N that stores the polynomial array, all elements are 0.
     */
    static long[] allocateZeroPolyArray(int polyCount, int coeffCount, int coeffUint64Count) {
        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;

        return new long[Common.mulSafe(polyCount, coeffCount, false, coeffUint64Count)];
    }

    /**
     * Sets the polynomial.
     *
     * @param poly             the polynomial.
     * @param coeffCount       number of coefficients N for each polynomial.
     * @param coeffUint64Count uint64 length l for each coefficient.
     * @param result           the result polynomial.
     */
    static void setPoly(long[] poly, int coeffCount, int coeffUint64Count, long[] result) {
        assert poly != null;
        assert result != null;
        assert coeffCount > 0 && coeffUint64Count > 0;

        UintCore.setUint(poly, Common.mulSafe(coeffCount, coeffUint64Count, false), result);
    }

    /**
     * Sets the polynomial array.
     *
     * @param poly             the polynomial.
     * @param polyCount        the number of polynomials m.
     * @param coeffCount       number of coefficients N for each polynomial.
     * @param coeffUint64Count uint64 length l for each coefficient.
     * @param result           the result polynomial.
     */
    static void setPolyArray(long[] poly, int polyCount, int coeffCount, int coeffUint64Count, long[] result) {
        assert polyCount > 0 && coeffCount > 0 && coeffUint64Count > 0;

        UintCore.setUint(poly, Common.mulSafe(coeffCount, coeffUint64Count, false, polyCount), result);
    }
}
