package edu.alibaba.mpc4j.s2pc.opf.oprf.prty19;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.HashMap;
import java.util.Map;

public class Prty19MpOprfUtils {
    /**
     * 私有构造函数。
     */
    private Prty19MpOprfUtils() {
        // empty
    }

    /**
     * W取值查找表
     */
    private static final Map<Integer, Integer> L_INIT_MATRIX = new HashMap<>();

    static {
        L_INIT_MATRIX.put(8, 412);
        L_INIT_MATRIX.put(9, 414);
        L_INIT_MATRIX.put(10, 416);
        L_INIT_MATRIX.put(11, 418);
        L_INIT_MATRIX.put(12, 420);
        L_INIT_MATRIX.put(13, 422);
        L_INIT_MATRIX.put(14, 424);
        L_INIT_MATRIX.put(15, 426);
        L_INIT_MATRIX.put(16, 428);
        L_INIT_MATRIX.put(17, 430);
        L_INIT_MATRIX.put(18, 432);
        L_INIT_MATRIX.put(19, 434);
        L_INIT_MATRIX.put(20, 436);
        L_INIT_MATRIX.put(21, 438);
        L_INIT_MATRIX.put(22, 440);
        L_INIT_MATRIX.put(23, 442);
        L_INIT_MATRIX.put(24, 444);
    }

    /**
     * 得到L的值，见原始论文表1。
     *
     * @param maxBatchSize 最大批处理数量。
     * @return w的值。
     */
    public static int getL(int maxBatchSize) {
        assert maxBatchSize > 0;
        // 支持的最小值为2^8
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 8));
        if (L_INIT_MATRIX.containsKey(nLogValue)) {
            return L_INIT_MATRIX.get(nLogValue);
        }
        throw new IllegalArgumentException(
                "MaxBatch Size = " + maxBatchSize + " exceeds supported size = " + (1 << 24));
    }
}
