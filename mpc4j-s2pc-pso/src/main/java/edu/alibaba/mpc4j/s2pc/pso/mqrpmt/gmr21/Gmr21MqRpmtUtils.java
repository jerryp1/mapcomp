package edu.alibaba.mpc4j.s2pc.pso.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;

/**
 * GMR21-mqRPMT协议工具类。
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
class Gmr21MqRpmtUtils {
    /**
     * 私有构造函数。
     */
    private Gmr21MqRpmtUtils() {
        // empty
    }

    /**
     * 有限域字节长度
     */
    static final int FINITE_FIELD_BYTE_LENGTH = Long.BYTES;

    /**
     * 计算PEQT协议对比字节长度σ + 2 * log_2(binNum)，转换为字节长度。
     *
     * @param binNum 桶数量（β）。
     * @return PEQT协议对比长度。
     */
    static int getPeqtByteLength(int binNum) {
        return CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(2 * (int) (DoubleUtils.log2(binNum)));
    }
}
