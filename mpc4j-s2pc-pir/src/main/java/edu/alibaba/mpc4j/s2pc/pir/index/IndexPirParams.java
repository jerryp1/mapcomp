package edu.alibaba.mpc4j.s2pc.pir.index;

/**
 * 索引PIR协议参数。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public interface IndexPirParams {
    /**
     * 返回最大检索数量。
     *
     * @return 最大检索数量。
     */
    int maxRetrievalSize();
}
