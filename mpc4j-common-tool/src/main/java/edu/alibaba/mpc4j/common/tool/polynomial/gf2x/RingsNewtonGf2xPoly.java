package edu.alibaba.mpc4j.common.tool.polynomial.gf2x;

import cc.redberry.rings.poly.univar.UnivariateInterpolation;
import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2x.Gf2xPolyFactory.Gf2xPolyType;

/**
 * 应用Rings Library实现的GF(2^L)牛顿多项式插值算法。
 *
 * @author Weiran Liu
 * @date 2021/12/26
 */
class RingsNewtonGf2xPoly extends AbstractRingsGf2xPoly {

    /**
     * 初始牛顿迭代插值GF(2^l)多项式运算。
     *
     * @param l GF(2^l)有限域比特长度。
     */
    RingsNewtonGf2xPoly(int l) {
        super(l);
    }

    @Override
    protected UnivariatePolynomial<UnivariatePolynomialZp64> polynomialInterpolate(
        int num, UnivariatePolynomialZp64[] xArray, UnivariatePolynomialZp64[] yArray) {
        // 插值
        return UnivariateInterpolation.interpolateNewton(this.finiteField, xArray, yArray);
    }

    @Override
    public Gf2xPolyType getGf2xPolyType() {
        return Gf2xPolyType.RINGS_NEWTON;
    }
}
