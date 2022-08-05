package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 本地NTL库GF(2^e)运算。
 *
 * @author Weiran Liu
 * @date 2022/5/18
 */
public class NtlGf2e implements Gf2e {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * GF(2^l)不可约多项式
     */
    private final byte[] minBytes;
    /**
     * l的值
     */
    private final int l;
    /**
     * l的字节长度
     */
    private final int byteL;
    /**
     * 0元
     */
    private final byte[] zero;
    /**
     * 1元
     */
    private final byte[] one;

    NtlGf2e(int l) {
        assert l > 0;
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        FiniteField<UnivariatePolynomialZp64> finiteField = Gf2eManager.getFiniteField(l);
        // 设置不可约多项式，系数个数为l + 1
        int minNum = l + 1;
        int minByteNum = CommonUtils.getByteLength(minNum);
        int minRoundBytes = minByteNum * Byte.SIZE;
        minBytes = new byte[minByteNum];
        UnivariatePolynomialZp64 minimalPolynomial = finiteField.getMinimalPolynomial();
        for (int i = 0; i <= minimalPolynomial.degree(); i++) {
            boolean coefficient = minimalPolynomial.get(i) != 0L;
            BinaryUtils.setBoolean(minBytes, minRoundBytes - 1 - i, coefficient);
        }
        zero = createZero();
        one = createOne();
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.NTL;
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
    public byte[] mul(byte[] a, byte[] b) {
        assert validateElement(a) && validateElement(b);
        return NtlNativeGf2e.nativeMul(minBytes, byteL, a, b);
    }

    @Override
    public void muli(byte[] a, byte[] b) {
        assert validateElement(a) && validateElement(b);
        NtlNativeGf2e.nativeMuli(minBytes, byteL, a, b);
    }

    @Override
    public byte[] div(byte[] a, byte[] b) {
        assert validateElement(a) && validateNonZeroElement(b);
        return NtlNativeGf2e.nativeDiv(minBytes, byteL, a, b);
    }

    @Override
    public void divi(byte[] a, byte[] b) {
        assert validateElement(a) && validateNonZeroElement(b);
        NtlNativeGf2e.nativeDivi(minBytes, byteL, a, b);
    }

    @Override
    public byte[] inv(byte[] a) {
        assert validateNonZeroElement(a);
        return NtlNativeGf2e.nativeInv(minBytes, byteL, a);
    }

    @Override
    public void invi(byte[] a) {
        assert validateNonZeroElement(a);
        NtlNativeGf2e.nativeInvi(minBytes, byteL, a);
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
    public byte[] createRandom(SecureRandom secureRandom) {
        return BytesUtils.randomByteArray(l, byteL, secureRandom);
    }

    @Override
    public byte[] createNonZeroRandom(SecureRandom secureRandom) {
        byte[] random = new byte[byteL];
        while (Arrays.equals(zero, random)) {
            random = BytesUtils.randomByteArray(l, byteL, secureRandom);
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
