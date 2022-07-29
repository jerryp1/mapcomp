package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.RingsUtils;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 用Rings实现的GF(2^l)运算。
 * <p>
 * 注意，{@code FiniteField<UnivariatePolynomialZp64>}下的运算不是线程安全的，需要增加synchronized关键字，这将严重影响计算效率。
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public class RingsGf2e implements Gf2e {
    /**
     * l比特长度
     */
    private final int l;
    /**
     * l字节长度
     */
    private final int byteL;
    /**
     * 有限域
     */
    private final FiniteField<UnivariatePolynomialZp64> finiteField;
    /**
     * 0元
     */
    private final byte[] zero;
    /**
     * 1元
     */
    private final byte[] one;

    RingsGf2e(int l) {
        assert l > 0;
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        finiteField = Gf2eManager.getFiniteField(l);
        zero = createZero();
        one = createOne();
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.RINGS;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getByteL() {
        return byteL;
    }

    @Override
    public synchronized byte[] mul(byte[] a, byte[] b) {
        assert validateElement(a) && validateElement(b);
        // 为与C层的表示保持一致，需要按照小端表示方法将字节数组转换为多项式，计算乘法后再按照小端表示转换回字节数组
        UnivariatePolynomialZp64 aPolynomial = RingsUtils.byteArrayToGf2e(a);
        UnivariatePolynomialZp64 bPolynomial = RingsUtils.byteArrayToGf2e(b);
        UnivariatePolynomialZp64 cPolynomial = finiteField.multiply(aPolynomial, bPolynomial);

        return RingsUtils.gf2eToByteArray(cPolynomial, byteL);
    }

    @Override
    public void muli(byte[] a, byte[] b) {
        byte[] c = mul(a, b);
        System.arraycopy(c, 0, a, 0, byteL);
    }

    @Override
    public synchronized byte[] div(byte[] a, byte[] b) {
        assert validateElement(a) && validateNonZeroElement(b);
        UnivariatePolynomialZp64 aPolynomial = RingsUtils.byteArrayToGf2e(a);
        UnivariatePolynomialZp64 bPolynomial = RingsUtils.byteArrayToGf2e(b);
        // c = a / b = a * (1 / b)
        UnivariatePolynomialZp64 cPolynomial = finiteField.multiply(
            aPolynomial, finiteField.divideExact(finiteField.getOne(), bPolynomial)
        );
        return RingsUtils.gf2eToByteArray(cPolynomial, byteL);
    }

    @Override
    public void divi(byte[] a, byte[] b) {
        byte[] c = div(a, b);
        System.arraycopy(c, 0, a, 0, byteL);
    }


    @Override
    public synchronized byte[] inv(byte[] a) {
        assert validateElement(a);
        UnivariatePolynomialZp64 aPolynomial = RingsUtils.byteArrayToGf2e(a);
        UnivariatePolynomialZp64 cPolynomial = finiteField.divideExact(finiteField.getOne(), aPolynomial);

        return RingsUtils.gf2eToByteArray(cPolynomial, byteL);
    }

    @Override
    public void invi(byte[] a) {
        byte[] c = inv(a);
        System.arraycopy(c, 0, a, 0, byteL);
    }

    @Override
    public byte[] createZero() {
        return new byte[byteL];
    }

    @Override
    public byte[] createOne() {
        byte[] one = new byte[byteL];
        one[one.length - 1] = (byte)0x01;
        return one;
    }

    @Override
    public byte[] randomElement(SecureRandom secureRandom) {
        byte[] random = new byte[byteL];
        secureRandom.nextBytes(random);
        BytesUtils.reduceByteArray(random, l);
        return random;
    }

    @Override
    public byte[] randomNonZeroElement(SecureRandom secureRandom) {
        byte[] random = new byte[byteL];
        while (Arrays.equals(zero, random)) {
            secureRandom.nextBytes(random);
            BytesUtils.reduceByteArray(random, l);
        }
        return random;
    }

    @Override
    public boolean isZero(byte[] a) {
        assert validateElement(a);
        return Arrays.equals(a, zero);
    }

    @Override
    public boolean isOne(byte[] a) {
        assert validateElement(a);
        return Arrays.equals(a, one);
    }

    @Override
    public boolean validateElement(byte[] a) {
        return a.length == byteL && BytesUtils.isReduceByteArray(a, l);
    }

    @Override
    public boolean validateNonZeroElement(byte[] a) {
        return a.length == byteL && BytesUtils.isReduceByteArray(a, l) && !Arrays.equals(a, zero);
    }
}
