package edu.alibaba.mpc4j.common.tool.galoisfield.zl;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * The Zl implemented by JDK.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
class JdkZl implements Zl {
    /**
     * the l bit length
     */
    private final int l;
    /**
     * the l byte length
     */
    private final int byteL;
    /**
     * module using AND operation
     */
    private final BigInteger andModule;
    /**
     * 2^l
     */
    private final BigInteger rangeBound;
    /**
     * the key derivation function
     */
    private final Kdf kdf;
    /**
     * the pseudo-random generator
     */
    private final Prg prg;

    JdkZl(EnvType envType, int l) {
        assert l > 0 : "l must be greater than 0";
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = BigInteger.ONE.shiftLeft(l);
        andModule = rangeBound.subtract(BigInteger.ONE);
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, byteL);
    }

    @Override
    public ZlFactory.ZlType getZlType() {
        return ZlFactory.ZlType.JDK;
    }

    @Override
    public BigInteger module(BigInteger a) {
        return a.and(andModule);
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
    public int getElementBitLength() {
        return l;
    }

    @Override
    public int getElementByteLength() {
        return byteL;
    }

    @Override
    public BigInteger getRangeBound() {
        return rangeBound;
    }

    @Override
    public BigInteger add(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.add(b).and(andModule);
    }

    @Override
    public BigInteger neg(final BigInteger a) {
        assert validateElement(a);
        if (a.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        } else {
            return rangeBound.subtract(a);
        }
    }

    @Override
    public BigInteger sub(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.subtract(b).and(andModule);
    }

    @Override
    public BigInteger mul(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return a.multiply(b).and(andModule);
    }

    @Override
    public BigInteger pow(final BigInteger a, final BigInteger b) {
        assert validateElement(a);
        assert validateElement(b);
        return BigIntegerUtils.modPow(a, b, rangeBound);
    }

    @Override
    public BigInteger innerProduct(final BigInteger[] elementVector, final boolean[] binaryVector) {
        assert elementVector.length == binaryVector.length
            : "element vector length must be equal to binary vector length = "
            + binaryVector.length + ": " + binaryVector.length;
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < elementVector.length; i++) {
            validateElement(elementVector[i]);
            if (binaryVector[i]) {
                value = add(value, elementVector[i]);
            }
        }
        return value;
    }

    @Override
    public BigInteger createZero() {
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger createOne() {
        return BigInteger.ONE;
    }

    @Override
    public boolean isZero(BigInteger a) {
        assert validateElement(a);
        return a.equals(BigInteger.ZERO);
    }

    @Override
    public boolean isOne(BigInteger a) {
        assert validateElement(a);
        return a.equals(BigInteger.ONE);
    }

    @Override
    public BigInteger createRandom(SecureRandom secureRandom) {
        return new BigInteger(l, secureRandom);
    }

    @Override
    public BigInteger createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray).and(andModule);
    }

    @Override
    public BigInteger createNonZeroRandom(SecureRandom secureRandom) {
        BigInteger random = BigInteger.ZERO;
        while (random.equals(BigInteger.ZERO)) {
            random = new BigInteger(l, secureRandom);
        }
        return random;
    }

    @Override
    public BigInteger createNonZeroRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        BigInteger random = BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray).and(andModule);
        while (random.equals(BigInteger.ZERO)) {
            // 如果恰巧为0，则迭代种子
            key = kdf.deriveKey(key);
            elementByteArray = prg.extendToBytes(key);
            random = BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray).and(andModule);
        }
        return random;
    }

    @Override
    public BigInteger createRangeRandom(SecureRandom secureRandom) {
        return new BigInteger(l, secureRandom);
    }

    @Override
    public BigInteger createRangeRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return BigIntegerUtils.byteArrayToNonNegBigInteger(elementByteArray).and(andModule);
    }

    @Override
    public boolean validateElement(final BigInteger a) {
        return a.signum() >= 0 && a.bitLength() <= l;
    }

    @Override
    public boolean validateNonZeroElement(final BigInteger a) {
        return a.signum() > 0 && a.bitLength() <= l;
    }

    @Override
    public boolean validateRangeElement(final BigInteger a) {
        return a.signum() >= 0 && a.bitLength() <= l;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JdkZl that = (JdkZl) o;
        // KDF and PRG can be different
        return this.l == that.l;
    }

    @Override
    public int hashCode() {
        return "Zl".hashCode();
    }
}
