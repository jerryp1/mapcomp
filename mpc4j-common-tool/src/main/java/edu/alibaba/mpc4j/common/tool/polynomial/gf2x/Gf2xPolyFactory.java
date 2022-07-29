package edu.alibaba.mpc4j.common.tool.polynomial.gf2x;

import edu.alibaba.mpc4j.common.tool.EnvType;

/**
 * GF(2^l)多项式插值工厂类。
 *
 * @author Weiran Liu
 * @date 2021/12/11
 */
public class Gf2xPolyFactory {
    /**
     * 私有构造函数。
     */
    private Gf2xPolyFactory() {
        // empty
    }

    /**
     * GF(2^l)多项式插值类型。
     */
    public enum Gf2xPolyType {
        /**
         * NTL库插值
         */
        NTL,
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
     * 创建GF(2^l)多项式插值实例。
     *
     * @param type 多项式插值类型。
     * @param l GF(2^l)有限域比特长度。
     * @return 多项式插值实例。
     */
    public static Gf2xPoly createInstance(Gf2xPolyType type, int l) {
        switch (type) {
            case NTL:
                return new NtlGf2xPoly(l);
            case RINGS_NEWTON:
                return new RingsNewtonGf2xPoly(l);
            case RINGS_LAGRANGE:
                return new RingsLagrangeGf2xPoly(l);
            default:
                throw new IllegalArgumentException("Invalid Gf2xPolyType: " + type.name());
        }
    }

    /**
     * 创建GF(2^l)多项式插值实例。
     *
     * @param envType 环境类型。
     * @param l GF(2^l)有限域比特长度。
     * @return 多项式插值实例。
     */
    public static Gf2xPoly createInstance(EnvType envType, int l) {
        switch (envType) {
            case STANDARD:
            case INLAND:
                return createInstance(Gf2xPolyType.NTL, l);
            case STANDARD_JDK:
            case INLAND_JDK:
                return createInstance(Gf2xPolyType.RINGS_NEWTON, l);
            default:
                throw new IllegalArgumentException("Invalid EnvType" + envType.name());
        }
    }
}
