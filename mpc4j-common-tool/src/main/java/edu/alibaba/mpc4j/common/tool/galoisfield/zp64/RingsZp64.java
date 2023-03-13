package edu.alibaba.mpc4j.common.tool.galoisfield.zp64;

import cc.redberry.rings.IntegersZp64;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 应用Rings实现的Zp64运算。
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
class RingsZp64 implements Zp64 {
    /**
     * the prime
     */
    private final long prime;
    /**
     * the prime bit length
     */
    private final int primeBitLength;
    /**
     * the prime byte length
     */
    private final int primeByteLength;
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
     * the finite field
     */
    private final IntegersZp64 integersZp64;
    /**
     * the key derivation function
     */
    private final Kdf kdf;
    /**
     * the pseudo-random generator
     */
    private final Prg prg;

    RingsZp64(EnvType envType, long prime) {
        assert BigInteger.valueOf(prime).isProbablePrime(CommonConstants.STATS_BIT_LENGTH)
            : "input prime is not a prime: " + prime;
        this.prime = prime;
        primeBitLength = LongUtils.ceilLog2(prime);
        primeByteLength = CommonUtils.getByteLength(primeBitLength);
        integersZp64 = new IntegersZp64(prime);
        l = primeBitLength - 1;
        byteL = CommonUtils.getByteLength(l);
        rangeBound = 1L << l;
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, Long.BYTES);
    }

    public RingsZp64(EnvType envType, int l) {
        prime = Zp64Manager.getPrime(l);
        primeBitLength = LongUtils.ceilLog2(prime);
        primeByteLength = CommonUtils.getByteLength(primeBitLength);
        this.l = l;
        integersZp64 = new IntegersZp64(prime);
        byteL = CommonUtils.getByteLength(l);
        rangeBound = 1L << l;
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, Long.BYTES);
    }

    @Override
    public Zp64Factory.Zp64Type getZp64Type() {
        return Zp64Factory.Zp64Type.RINGS;
    }

    @Override
    public long getPrime() {
        return prime;
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
        return primeBitLength;
    }

    @Override
    public int getElementByteLength() {
        return primeByteLength;
    }

    @Override
    public long getRangeBound() {
        return rangeBound;
    }

    @Override
    public long module(final long a) {
        return integersZp64.modulus(a);
    }

    @Override
    public long add(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return integersZp64.add(a, b);
    }

    @Override
    public long neg(final long a) {
        assert validateElement(a);
        return integersZp64.negate(a);
    }

    @Override
    public long sub(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return integersZp64.subtract(a, b);
    }

    @Override
    public long mul(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return integersZp64.multiply(a, b);
    }

    @Override
    public long div(final long a, final long b) {
        assert validateElement(a);
        assert validateNonZeroElement(b);
        return integersZp64.divide(a, b);
    }

    @Override
    public long inv(final long a) {
        assert validateNonZeroElement(a);
        return integersZp64.divide(1L, a);
    }

    @Override
    public long pow(final long a, final long b) {
        assert validateElement(a);
        assert validateElement(b);
        return integersZp64.powMod(a, b);
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
        return LongUtils.randomNonNegative(prime, secureRandom);
    }

    @Override
    public long createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        return Math.abs(LongUtils.byteArrayToLong(elementByteArray) % prime);
    }

    @Override
    public long createNonZeroRandom(SecureRandom secureRandom) {
        long random = 0L;
        while (random == 0L) {
            random = LongUtils.randomPositive(prime, secureRandom);
        }
        return random;
    }

    @Override
    public long createNonZeroRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] elementByteArray = prg.extendToBytes(key);
        long random = Math.abs(LongUtils.byteArrayToLong(elementByteArray) % prime);
        while (random == 0L) {
            // 如果恰巧为0，则迭代种子
            key = kdf.deriveKey(key);
            elementByteArray = prg.extendToBytes(key);
            random = Math.abs(LongUtils.byteArrayToLong(elementByteArray) % prime);
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
        return a >= 0 && a < prime;
    }

    @Override
    public boolean validateNonZeroElement(final long a) {
        return a > 0 && a < prime;
    }

    @Override
    public boolean validateRangeElement(final long a) {
        return a >= 0 && a < rangeBound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RingsZp64 that = (RingsZp64) o;
        // KDF and PRG can be different
        return this.prime == that.prime;
    }

    @Override
    public int hashCode() {
        return new Long(prime).hashCode();
    }
}
