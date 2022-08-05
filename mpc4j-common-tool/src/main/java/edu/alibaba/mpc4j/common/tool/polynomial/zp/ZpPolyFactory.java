package edu.alibaba.mpc4j.common.tool.polynomial.zp;

/**
 * Zp多项式插值工厂类。
 *
 * @author Weiran Liu
 * @date 2021/06/05
 */
public class ZpPolyFactory {
    /**
     * 私有构造函数。
     */
    private ZpPolyFactory() {
        // empty
    }

    /**
     * Zp多项式插值类型。
     */
    public enum ZpPolyType {
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
     * 创建多项式插值实例。
     *
     * @param type 多项式插值类型。
     * @param l    有限域比特长度。
     * @return 多项式插值实例。
     */
    public static ZpPoly createInstance(ZpPolyType type, int l) {
        switch (type) {
            case NTL:
                return new NtlZpPoly(l);
            case RINGS_NEWTON:
                return new RingsNewtonZpPoly(l);
            case RINGS_LAGRANGE:
                return new RingsLagrangeZpPoly(l);
            default:
                throw new IllegalArgumentException("Invalid ZpPolyType: " + type.name());
        }
    }
}
