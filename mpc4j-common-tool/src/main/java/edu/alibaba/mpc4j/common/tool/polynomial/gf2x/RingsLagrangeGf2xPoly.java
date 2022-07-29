package edu.alibaba.mpc4j.common.tool.polynomial.gf2x;

import cc.redberry.rings.poly.univar.UnivariateInterpolation;
import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import com.google.common.base.Preconditions;

/**
 * 应用Rings Library实现的GF(2^L)拉格朗日多项式插值算法。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
class RingsLagrangeGf2xPoly extends AbstractRingsGf2xPoly {

    /**
     * 初始拉格朗日插值GF(2^l)多项式运算。
     *
     * @param l GF(2^l)有限域比特长度。
     */
    public RingsLagrangeGf2xPoly(int l) {
        super(l);
    }

    @Override
    public UnivariatePolynomial<UnivariatePolynomialZp64> polynomialInterpolate(
        int num, UnivariatePolynomialZp64[] xArray, UnivariatePolynomialZp64[] yArray) {
        Preconditions.checkArgument(xArray.length == yArray.length);
        Preconditions.checkArgument(xArray.length <= num);
        // 插值
        return UnivariateInterpolation.interpolateLagrange(finiteField, xArray, yArray);
    }

    @Override
    public Gf2xPolyFactory.Gf2xPolyType getGf2xPolyType() {
        return Gf2xPolyFactory.Gf2xPolyType.RINGS_LAGRANGE;
    }
}
