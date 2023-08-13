package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.common.tool.coder.random.RandomCoder;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/**
 * 记录一些待解决的问题：
 *  1. Polynomial 需要包含 coeff_modulus 这个字段吗？即对 coeffs 的值 需要有约束吗？
 *     py-fhe 的实现是没有的，是在每一个实现时，传入 coeffsModulus
 *     我认为也是不需要的，
 *  2. coeffModulus 需要是一个 素数吗？好像也不一定，只有在使用 NTT 的时候，才对系数有要求
 *  3. 但是另一方面来说，我这里的多项式其实很明确，就是 Z_Q[x] / (x^n + 1) 下的多项式
 *
 *  4. 如何处理负数？即 如何处理 负数 mod 模数？是就让其保持负数？还是映射回 [0, Q-1] 范围？
 *     4.1 测试了 py-fhe 中的做法，多项式加法和乘法这一个范围内是将其还原为了 [0, Q-1] 这个范围内的
 *
 *
 * @author Qixian Zhou
 * @date 2023/7/12
 */
public class Polynomial {


    public long polyModulusDegree;

    public long[] coeffs;

    public double[] doubleCoeffs;


    public Polynomial(long polyModulusDegree, long[] coeffs) {

        assert polyModulusDegree == coeffs.length: "Size of polynomial array must be equal ring degree.";
        // todo
        // 需要判断 ringDegree 是 2 的整数次幂吗？
        // 这里仅仅是多项式本身，认为可以不做判断
        this.polyModulusDegree = polyModulusDegree;
        this.coeffs = coeffs;
    }


    public Polynomial(long polyModulusDegree, double[] doubleCoeffs) {

        assert polyModulusDegree == coeffs.length: "Size of polynomial array must be equal ring degree.";
        // todo
        // 需要判断 ringDegree 是 2 的整数次幂吗？
        // 这里仅仅是多项式本身，认为可以不做判断
        this.polyModulusDegree = polyModulusDegree;
        this.doubleCoeffs = doubleCoeffs;
    }


    public Polynomial add(Polynomial poly, long coeffsModulus) {

        assert this.polyModulusDegree == poly.polyModulusDegree;

        long[] result = new long[(int) this.polyModulusDegree];
        for (int i = 0; i < this.polyModulusDegree; i++) {
            result[i] = (this.coeffs[i] + poly.coeffs[i]) % coeffsModulus;
            if (result[i] < 0) {
                result[i] += coeffsModulus;
            }
        }
        return new Polynomial(polyModulusDegree, result);
    }

    public Polynomial add(Polynomial poly) {

        assert this.polyModulusDegree == poly.polyModulusDegree;

        long[] result = new long[(int) this.polyModulusDegree];
        for (int i = 0; i < this.polyModulusDegree; i++) {
            result[i] = this.coeffs[i] + poly.coeffs[i];
        }
        return new Polynomial(polyModulusDegree, result);
    }

    public Polynomial sub(Polynomial poly, long coeffsModulus) {

        assert this.polyModulusDegree == poly.polyModulusDegree;

        long[] result = new long[(int) this.polyModulusDegree];
        for (int i = 0; i < this.polyModulusDegree; i++) {
            result[i] = (this.coeffs[i] - poly.coeffs[i]) % coeffsModulus;
            if (result[i] < 0) {
                result[i] += coeffsModulus;
            }
        }
        return new Polynomial(polyModulusDegree, result);
    }


    public Polynomial mul(Polynomial poly, long coeffsModulus) {

        assert this.polyModulusDegree == poly.polyModulusDegree;
        return mulNaive(poly, coeffsModulus);
    }

    public Polynomial mul(Polynomial poly, NttContext nttContext) {

        assert poly.polyModulusDegree == this.polyModulusDegree;
        return mulNtt(poly, nttContext);
    }

    public BigInteger[] mul(Polynomial poly, CrtContext crtContext) {
        assert poly.polyModulusDegree == this.polyModulusDegree;
        return mutCrt(poly, crtContext);
    }

    private BigInteger[] mutCrt(Polynomial poly, CrtContext crtContext) {

        assert poly.polyModulusDegree == crtContext.polyModulusDegree;

        Polynomial[] polyProds = new Polynomial[crtContext.numPrimes];
        for (int i = 0; i < crtContext.numPrimes; i++) {
            Polynomial prod = mulNtt(poly, crtContext.nttContexts[i]);
            polyProds[i] = prod;
        }
        // 这里暂时用 long 吧，因为暂时我们的数据规模会比较小
        BigInteger[] finalCoeffs = new BigInteger[(int) polyModulusDegree];
        for (int i = 0; i < polyModulusDegree; i++) {
            long[] values = new long[crtContext.numPrimes];
            for (int j = 0; j < polyProds.length; j++) {
                values[j] = polyProds[j].coeffs[i];
            }
            finalCoeffs[i] = crtContext.reconstruct(values);
        }
        return finalCoeffs;
    }

    private Polynomial mulNtt(Polynomial poly, NttContext nttContext) {

        assert this.polyModulusDegree == poly.polyModulusDegree;

        long[] a = nttContext.nttForward(this.coeffs);
        long[] b = nttContext.nttForward(poly.coeffs);

        long[] ab = new long[a.length];
        for (int i = 0; i < a.length; i++) {
            ab[i] = nttContext.ringLweZp64.mul(a[i], b[i]);
        }
        ab = nttContext.nttInverse(ab);

        return new Polynomial(polyModulusDegree, ab);
    }



    private Polynomial mulNaive(Polynomial poly, long coeffsModulus) {

        assert this.polyModulusDegree == poly.polyModulusDegree;

        Polynomial polyProd = new Polynomial(polyModulusDegree, new long[(int) polyModulusDegree]);

        for (int d = 0; d < 2 * polyModulusDegree - 1; d++) {

            int index = (int) (d % this.polyModulusDegree);
            int sign = d < this.polyModulusDegree ? 1: -1;

            long coeff = 0;
            for (int i = 0; i < polyModulusDegree; i++) {
                if (0 <= d - i && d - i < polyModulusDegree) {
                    coeff += (this.coeffs[i] * poly.coeffs[d -i]) % coeffsModulus;
                }
            }
            polyProd.coeffs[index] += (sign * coeff) % coeffsModulus;
        }
        // mainly want to handel the possible negative coeffs
        polyProd.modInplace(coeffsModulus);
        return polyProd;
    }

    public Polynomial mulNaiveNoMod(Polynomial poly) {

        assert this.polyModulusDegree == poly.polyModulusDegree;

        Polynomial polyProd = new Polynomial(polyModulusDegree, new long[(int) polyModulusDegree]);

        for (int d = 0; d < 2 * polyModulusDegree - 1; d++) {

            int index = (int) (d % this.polyModulusDegree);
            int sign = d < this.polyModulusDegree ? 1: -1;

            long coeff = 0;
            for (int i = 0; i < polyModulusDegree; i++) {
                if (0 <= d - i && d - i < polyModulusDegree) {
                    coeff += (this.coeffs[i] * poly.coeffs[d -i]) ;
                }
            }
            polyProd.coeffs[index] += (sign * coeff);
        }
        return polyProd;
    }


    public Polynomial mulScalar(long scalar) {
        long[] newCoeffs = new long[(int) this.polyModulusDegree];

        for (int i = 0; i < this.polyModulusDegree; i++) {
            newCoeffs[i] = scalar * this.coeffs[i];
        }
        return new Polynomial(this.polyModulusDegree, newCoeffs);
    }
    // 就近取整
    // round(2.5) = 3, round(2.4) = 2
    // round(0.5) = 0
    // round(-1.5) = -1
    // |x| = n + 0.5 , round |x| = n
    public Polynomial round(double[] doubleCoeffs) {
        long[] coeffs = new long[doubleCoeffs.length];

        for (int i = 0; i < doubleCoeffs.length; i++) {
            coeffs[i] = (long) Math.round(doubleCoeffs[i]);
        }
        return new Polynomial(doubleCoeffs.length, coeffs);
    }

    public Polynomial floor(double[] doubleCoeffs) {
        long[] coeffs = new long[doubleCoeffs.length];

        for (int i = 0; i < doubleCoeffs.length; i++) {
            coeffs[i] = (long) Math.floor(doubleCoeffs[i]);
        }
        return new Polynomial(doubleCoeffs.length, coeffs);
    }

    public double[] mulDoubleScalar(double scalar) {
        double[] newCoeffs = new double[(int) this.polyModulusDegree];

        for (int i = 0; i < this.polyModulusDegree; i++) {
            newCoeffs[i] = scalar * this.coeffs[i];
        }
        return newCoeffs;
    }

    public Polynomial mulScalarRound(double scalar) {
        double[] newCoeffs = new double[(int) this.polyModulusDegree];

        for (int i = 0; i < this.polyModulusDegree; i++) {
            newCoeffs[i] = scalar * this.coeffs[i];
        }
        return round(newCoeffs);
    }

    public Polynomial mulScalarFloor(double scalar) {
        double[] newCoeffs = new double[(int) this.polyModulusDegree];

        for (int i = 0; i < this.polyModulusDegree; i++) {
            newCoeffs[i] = scalar * this.coeffs[i];
        }
        return floor(newCoeffs);
    }

    public void mulScalarFloorInplace(double scalar) {
        for (int i = 0; i < this.polyModulusDegree; i++) {
            coeffs[i] = (long) (scalar * this.coeffs[i]);
        }
    }


    public Polynomial mulScalar(long scalar, long coeffModulus) {

        long[] newCoeffs = new long[(int) this.polyModulusDegree];

        for (int i = 0; i < this.polyModulusDegree; i++) {

            long tmp = (scalar * this.coeffs[i]) % coeffModulus;
            if (tmp < 0) {
                tmp %= coeffModulus;
            }
            newCoeffs[i] = tmp;
        }
        return new Polynomial(this.polyModulusDegree, newCoeffs);
    }

    /**
     * convert coeff to the range [0, modulus - 1]
     * @param coeffModulus coeff modulus
     */
    public void modInplace(long coeffModulus) {
        for (int i = 0; i < this.polyModulusDegree; i++) {
            if (coeffs[i] < 0) {
                coeffs[i] %= coeffModulus;
                coeffs[i] += coeffModulus;
            }else{
                coeffs[i] %= coeffModulus;
            }
        }
    }

    public Polynomial mod(long coeffModulus) {
        long[] newCoeffs = new long[(int) polyModulusDegree];
        for (int i = 0; i < this.polyModulusDegree; i++) {
            if (coeffs[i] < 0) {
                newCoeffs[i] = (coeffs[i] % coeffModulus) + coeffModulus;
            }else {
                // 这里千万不能忘了
                newCoeffs[i] = coeffs[i] % coeffModulus;
            }
        }
        return new Polynomial(polyModulusDegree, newCoeffs);
    }


    public Polynomial[] baseDecompose(long base, int numLevels) {

        Polynomial[] decomposed = new Polynomial[numLevels];
        for (int i = 0; i < numLevels; i++) {
            decomposed[i] = this.mod(base);
            this.mulScalarFloorInplace((double) 1/base);
        }
        return decomposed;
    }




    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Polynomial)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        Polynomial that = (Polynomial)obj;
        return new EqualsBuilder()
                .append(this.polyModulusDegree, that.polyModulusDegree)
                .append(this.coeffs, that.coeffs)
                .isEquals();
    }



    @Override
    public int hashCode() {
        int result = Objects.hash(polyModulusDegree);
        result = 31 * result + Arrays.hashCode(coeffs);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = coeffs.length - 1; i > 0; i--) {
            sb.append(coeffs[i]);
            sb.append("x^");
            sb.append(i);
            sb.append(" + ");
        }
        sb.append(coeffs[0]);
        return sb.toString();
    }
}
