package edu.alibaba.mpc4j.s2pc.pso.upsi;

/**
 * 非平衡PSI协议参数。
 *
 * @author Weiran Liu
 * @date 2022/8/7
 */
public interface UpsiParams {
    /**
     * 返回最大客户端元素数量。
     *
     * @return 最大客户端元素数量。
     */
    int maxClientElementSize();
}
