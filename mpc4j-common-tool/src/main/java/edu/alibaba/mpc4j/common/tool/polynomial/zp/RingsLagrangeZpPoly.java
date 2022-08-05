package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import cc.redberry.rings.poly.univar.UnivariateInterpolation;
import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import edu.alibaba.mpc4j.common.tool.polynomial.zp.ZpPolyFactory.ZpPolyType;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * 应用Rings实现的Zp拉格朗日多项式插值。
 *
 * @author Weiran Liu
 * @date 2021/05/31
 */
class RingsLagrangeZpPoly extends AbstractRingsZpPoly {

    RingsLagrangeZpPoly(int l) {
        super(l);
    }

    @Override
    public ZpPolyType getType() {
        return ZpPolyType.RINGS_LAGRANGE;
    }

    @Override
    protected UnivariatePolynomial<cc.redberry.rings.bigint.BigInteger> polynomialInterpolate(int num,
        BigInteger[] xArray, BigInteger[] yArray) {
        // 转换成多项式点
        cc.redberry.rings.bigint.BigInteger[] points = Arrays.stream(xArray)
            .map(cc.redberry.rings.bigint.BigInteger::new)
            .toArray(cc.redberry.rings.bigint.BigInteger[]::new);
        cc.redberry.rings.bigint.BigInteger[] values = Arrays.stream(yArray)
            .map(cc.redberry.rings.bigint.BigInteger::new)
            .toArray(cc.redberry.rings.bigint.BigInteger[]::new);
        // 插值
        return UnivariateInterpolation.interpolateLagrange(finiteField, points, values);
    }
}
