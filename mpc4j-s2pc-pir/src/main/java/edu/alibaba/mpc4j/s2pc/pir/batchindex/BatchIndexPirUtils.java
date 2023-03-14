package edu.alibaba.mpc4j.s2pc.pir.batchindex;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 批索引PIR协议工具类。
 *
 * @author Liqiang Peng
 * @date 2023/3/14
 */
public class BatchIndexPirUtils {

    /**
     * 明文多项式移位。
     *
     * @param coeffs 多项式系数。
     * @param offset 移位。
     * @return 移位后的多项式。
     */
    public static long[][] plaintextRotate(long[][] coeffs, int offset) {
        return Arrays.stream(coeffs).map(coeff -> plaintextRotate(coeff, offset)).toArray(long[][]::new);
    }

    /**
     * 明文多项式移位。
     *
     * @param coeffs 多项式系数。
     * @param offset 移位。
     * @return 移位后的多项式。
     */
    public static long[] plaintextRotate(long[] coeffs, int offset) {
        int rowCount = coeffs.length / 2;
        long[] rotatedCoeffs = new long[coeffs.length];
        IntStream.range(0, rowCount).forEach(j -> rotatedCoeffs[j] = coeffs[(rowCount - offset + j) % rowCount]);
        return rotatedCoeffs;
    }
}
