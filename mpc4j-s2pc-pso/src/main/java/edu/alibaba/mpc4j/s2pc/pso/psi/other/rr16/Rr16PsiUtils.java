package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.HashMap;
import java.util.Map;

public class Rr16PsiUtils {
    /**
     * 私有构造函数。
     */
    private Rr16PsiUtils() {
        // empty
    }

    /**
     * Not取值查找表
     */
    private static final Map<Integer, Integer> N_OT_INIT_MATRIX = new HashMap<>();

    static {
        N_OT_INIT_MATRIX.put(2, 8295);
        N_OT_INIT_MATRIX.put(3, 10663);
        N_OT_INIT_MATRIX.put(4, 14715);
        N_OT_INIT_MATRIX.put(5, 21762);
        N_OT_INIT_MATRIX.put(6, 34286);
        N_OT_INIT_MATRIX.put(7, 57068);
        N_OT_INIT_MATRIX.put(8, 99372);
        N_OT_INIT_MATRIX.put(9, 179281);
        N_OT_INIT_MATRIX.put(10, 331450);
        N_OT_INIT_MATRIX.put(11, 623180);
        N_OT_INIT_MATRIX.put(12, 1187141);
        N_OT_INIT_MATRIX.put(13, 2285265);
        N_OT_INIT_MATRIX.put(14, 4434188);
        N_OT_INIT_MATRIX.put(15, 8658560);
        N_OT_INIT_MATRIX.put(16, 16992857);
        N_OT_INIT_MATRIX.put(17, 33479820);
        N_OT_INIT_MATRIX.put(18, 66165163);
        N_OT_INIT_MATRIX.put(19, 131108816);
        N_OT_INIT_MATRIX.put(20, 260252093);
        N_OT_INIT_MATRIX.put(21, 517435654);
        N_OT_INIT_MATRIX.put(22, 1030082690);
        N_OT_INIT_MATRIX.put(23, 2052497778);
    }

    /**
     * 得到Not的值，见https://github.com/osu-crypto/libPSI/。
     *
     * @param maxBatchSize 最大批处理数量。
     * @return w的值。
     */
    public static int getOtBatchSize(int maxBatchSize) {
        assert maxBatchSize > 0;
        // 支持的最小值为2^2
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 2));
        if (N_OT_INIT_MATRIX.containsKey(nLogValue)) {
            return N_OT_INIT_MATRIX.get(nLogValue);
        }
        throw new IllegalArgumentException(
            "MaxBatch Size = " + maxBatchSize + " exceeds supported size = " + (1 << 23));
    }

    /**
     * Notone取值查找表
     */
    private static final Map<Integer, Integer> N_ONE_INIT_MATRIX = new HashMap<>();

    static {
        N_ONE_INIT_MATRIX.put(2, 517);
        N_ONE_INIT_MATRIX.put(3, 959);
        N_ONE_INIT_MATRIX.put(4, 1829);
        N_ONE_INIT_MATRIX.put(5, 3549);
        N_ONE_INIT_MATRIX.put(6, 6961);
        N_ONE_INIT_MATRIX.put(7, 13743);
        N_ONE_INIT_MATRIX.put(8, 27246);
        N_ONE_INIT_MATRIX.put(9, 54101);
        N_ONE_INIT_MATRIX.put(10, 105905);
        N_ONE_INIT_MATRIX.put(11, 207952);
        N_ONE_INIT_MATRIX.put(12, 407982);
        N_ONE_INIT_MATRIX.put(13, 790117);
        N_ONE_INIT_MATRIX.put(14, 1582849);
        N_ONE_INIT_MATRIX.put(15, 3073716);
        N_ONE_INIT_MATRIX.put(16, 6113957);
        N_ONE_INIT_MATRIX.put(17, 12162968);
        N_ONE_INIT_MATRIX.put(18, 23957707);
        N_ONE_INIT_MATRIX.put(19, 47283348);
        N_ONE_INIT_MATRIX.put(20, 95333932);
        N_ONE_INIT_MATRIX.put(21, 188162824);
        N_ONE_INIT_MATRIX.put(22, 371340163);
        N_ONE_INIT_MATRIX.put(23, 758786092);
    }

    /**
     * 得到Notone的值，见https://github.com/osu-crypto/libPSI/。
     *
     * @param maxBatchSize 最大批处理数量。
     * @return w的值。
     */
    public static int getOtOneCount(int maxBatchSize) {
        assert maxBatchSize > 0;
        // 支持的最小值为2^2
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 2));
        if (N_ONE_INIT_MATRIX.containsKey(nLogValue)) {
            return N_ONE_INIT_MATRIX.get(nLogValue);
        }
        throw new IllegalArgumentException(
            "MaxBatch Size = " + maxBatchSize + " exceeds supported size = " + (1 << 23));
    }

    /**
     * CncThreshold取值查找表
     */
    private static final Map<Integer, Integer> T_CNC_INIT_MATRIX = new HashMap<>();

    static {
        T_CNC_INIT_MATRIX.put(2, 138);
        T_CNC_INIT_MATRIX.put(3, 204);
        T_CNC_INIT_MATRIX.put(4, 323);
        T_CNC_INIT_MATRIX.put(5, 540);
        T_CNC_INIT_MATRIX.put(6, 944);
        T_CNC_INIT_MATRIX.put(7, 1711);
        T_CNC_INIT_MATRIX.put(8, 3182);
        T_CNC_INIT_MATRIX.put(9, 5973);
        T_CNC_INIT_MATRIX.put(10, 9648);
        T_CNC_INIT_MATRIX.put(11, 15440);
        T_CNC_INIT_MATRIX.put(12, 22958);
        T_CNC_INIT_MATRIX.put(13, 36452);
        T_CNC_INIT_MATRIX.put(14, 59137);
        T_CNC_INIT_MATRIX.put(15, 91828);
        T_CNC_INIT_MATRIX.put(16, 150181);
        T_CNC_INIT_MATRIX.put(17, 235416);
        T_CNC_INIT_MATRIX.put(18, 364747);
        T_CNC_INIT_MATRIX.put(19, 621716);
        T_CNC_INIT_MATRIX.put(20, 962092);
        T_CNC_INIT_MATRIX.put(21, 1516296);
        T_CNC_INIT_MATRIX.put(22, 2241411);
        T_CNC_INIT_MATRIX.put(23, 3811372);
    }

    /**
     * 得到Threshold的值，见https://github.com/osu-crypto/libPSI/。
     *
     * @param maxBatchSize 最大批处理数量。
     * @return w的值。
     */
    public static int getCncThreshold(int maxBatchSize) {
        assert maxBatchSize > 0;
        // 支持的最小值为2^2
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 2));
        if (T_CNC_INIT_MATRIX.containsKey(nLogValue)) {
            return T_CNC_INIT_MATRIX.get(nLogValue);
        }
        throw new IllegalArgumentException(
            "MaxBatch Size = " + maxBatchSize + " exceeds supported size = " + (1 << 23));
    }

    /**
     * CncProb取值查找表
     */
    private static final Map<Integer, Double> P_CNC_INIT_MATRIX = new HashMap<>();

    static {
        P_CNC_INIT_MATRIX.put(2, 0.099);
        P_CNC_INIT_MATRIX.put(3, 0.099);
        P_CNC_INIT_MATRIX.put(4, 0.099);
        P_CNC_INIT_MATRIX.put(5, 0.099);
        P_CNC_INIT_MATRIX.put(6, 0.099);
        P_CNC_INIT_MATRIX.put(7, 0.099);
        P_CNC_INIT_MATRIX.put(8, 0.099);
        P_CNC_INIT_MATRIX.put(9, 0.098);
        P_CNC_INIT_MATRIX.put(10, 0.083);
        P_CNC_INIT_MATRIX.put(11, 0.069);
        P_CNC_INIT_MATRIX.put(12, 0.053);
        P_CNC_INIT_MATRIX.put(13, 0.044);
        P_CNC_INIT_MATRIX.put(14, 0.036);
        P_CNC_INIT_MATRIX.put(15, 0.029);
        P_CNC_INIT_MATRIX.put(16, 0.024);
        P_CNC_INIT_MATRIX.put(17, 0.019);
        P_CNC_INIT_MATRIX.put(18, 0.015);
        P_CNC_INIT_MATRIX.put(19, 0.013);
        P_CNC_INIT_MATRIX.put(20, 0.01);
        P_CNC_INIT_MATRIX.put(21, 0.008);
        P_CNC_INIT_MATRIX.put(22, 0.006);
        P_CNC_INIT_MATRIX.put(23, 0.005);
    }

    /**
     * 得到Prob的值，见https://github.com/osu-crypto/libPSI/。
     *
     * @param maxBatchSize 最大批处理数量。
     * @return w的值。
     */
    public static double getCncProb(int maxBatchSize) {
        assert maxBatchSize > 0;
        // 支持的最小值为2^2
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 2));
        if (P_CNC_INIT_MATRIX.containsKey(nLogValue)) {
            return P_CNC_INIT_MATRIX.get(nLogValue);
        }
        throw new IllegalArgumentException(
            "MaxBatch Size = " + maxBatchSize + " exceeds supported size = " + (1 << 23));
    }
}
