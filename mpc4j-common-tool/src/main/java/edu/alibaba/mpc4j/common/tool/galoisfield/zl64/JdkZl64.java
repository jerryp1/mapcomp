package edu.alibaba.mpc4j.common.tool.galoisfield.zl64;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.security.SecureRandom;

/**
 * Zl64 implemented by JDK.
 *
 * @author Weiran Liu
 * @date 2023/2/20
 */
class JdkZl64 implements Zl64 {
    /**
     * the l bit length
     */
    private final int l;
    /**
     * the l byte length
     */
    private final int byteL;
    /**
     * 2^l
     */
    private final long rangeBound;
    /**
     * module using AND operation
     */
    private final long andModule;
    /**
     * the key derivation function
     */
    private final Kdf kdf;
    /**
     * the pseudo-random generator
     */
    private final Prg prg;

    public JdkZl64(EnvType envType, int l) {
        assert l > 0 : "l must be greater than 0";
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = 1L << l;
        andModule = rangeBound - 1;
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, Long.BYTES);
    }

    @Override
    public Zl64Factory.Zl64Type getZl64Type() {
        return Zl64Factory.Zl64Type.JDK;
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
    public long getRangeBound() {
        return rangeBound;
    }

    @Override
    public long module(final long a) {
        return a & andModule;
    }

    @Override
    public long add(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return (a + b) & andModule;
    }

    @Override
    public long neg(final long a) {
        assert validateElement(a);
        return (-a) & andModule;
    }

    @Override
    public long sub(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return (a - b) & andModule;
    }

    @Override
    public long mul(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return (a * b) & andModule;
    }

    @Override
    public long pow(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        // this is exactly what Rings did. However, since the module is 2^l, we can use & instead of mod.
        if (b == 0) {
            return 1;
        }
        long result = 1;
        long exponent = b;
        long base2k = a;
        for (; ; ) {
            if ((exponent & 1) != 0) {
                result = (result * base2k) & andModule;
            }
            exponent = exponent >> 1;
            if (exponent == 0) {
                return result;
            }
            base2k = (base2k * base2k) & andModule;
        }
    }

    @Override
    public long createZero() {
        return 0L;
    }

    @Override
    public long createOne() {
        return 1L;
    }

    @Override
    public boolean isZero(final long a) {
        assert validateElement(a);
        return a == 0L;
    }

    @Override
    public boolean isOne(final long a) {
        assert validateElement(a);
        return a == 1L;
    }

    @Override
    public long createRandom(SecureRandom secureRandom) {
        return LongUtils.randomNonNegative(rangeBound, secureRandom);
    }

    @Override
    public long createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return Math.abs(LongUtils.byteArrayToLong(elementByteArray) % rangeBound);
    }

    @Override
    public long createNonZeroRandom(SecureRandom secureRandom) {
        long random = 0L;
        while (random == 0L) {
            random = LongUtils.randomPositive(rangeBound, secureRandom);
        }
        return random;
    }

    @Override
    public long createNonZeroRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        long random = Math.abs(LongUtils.byteArrayToLong(elementByteArray) % rangeBound);
        while (random == 0L) {
            // 如果恰巧为0，则迭代种子
            key = kdf.deriveKey(key);
            elementByteArray = prg.extendToBytes(key);
            random = Math.abs(LongUtils.byteArrayToLong(elementByteArray) % rangeBound);
        }
        return random;
    }

    @Override
    public long createRangeRandom(SecureRandom secureRandom) {
        return LongUtils.randomNonNegative(rangeBound, secureRandom);
    }

    @Override
    public long createRangeRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return Math.abs(LongUtils.byteArrayToLong(elementByteArray) % rangeBound);
    }

    @Override
    public boolean validateElement(final long a) {
        return a >= 0 && a < rangeBound;
    }

    @Override
    public boolean validateNonZeroElement(final long a) {
        return a > 0 && a < rangeBound;
    }

    @Override
    public boolean validateRangeElement(final long a) {
        return a >= 0 && a < rangeBound;
    }
}
