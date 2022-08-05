package edu.alibaba.mpc4j.common.tool.polynomial.zp64;

import edu.alibaba.mpc4j.common.tool.polynomial.zp.*;

/**
 * Zp64多项式插值工厂类。
 *
 * @author Weiran Liu
 * @date 2022/8/3
 */
public class Zp64PolyFactory {
    /**
     * 私有构造函数。
     */
    private Zp64PolyFactory() {
        // empty
    }

    /**
     * Zp64多项式插值类型。
     */
    public enum Zp64PolyType {
        /**
         * Rings实现的拉格朗日插值
         */
        RINGS_LAGRANGE,
        /**
         * Rings实现的牛顿插值
         */
        RINGS_NEWTON,
    }

    /**
     * 创建多项式插值实例。
     *
     * @param type 多项式插值类型。
     * @param l    有限域比特长度。
     * @return 多项式插值实例。
     */
    public static Zp64Poly createInstance(Zp64PolyType type, int l) {
        switch (type) {
            case RINGS_NEWTON:
                return new RingsNewtonZp64Poly(l);
            case RINGS_LAGRANGE:
                return new RingsLagrangeZp64Poly(l);
            default:
                throw new IllegalArgumentException("Invalid Zp64PolyType: " + type.name());
        }
    }
}
