package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.security.SecureRandom;

/**
 * Abstract GF(2^k) interface.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
abstract class AbstractGf2k implements Gf2k {
    /**
     * l = λ (in bit length)
     */
    private static final int L = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * l = λ (in byte length)
     */
    private static final int BYTE_L = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * the zero element
     */
    protected final byte[] zero;
    /**
     * the identity element
     */
    protected final byte[] one;
    /**
     * the key derivation function
     */
    private final Kdf kdf;
    /**
     * the pseudo-random generator
     */
    private final Prg prg;

    public AbstractGf2k(EnvType envType) {
        zero = createZero();
        one = createOne();
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, BYTE_L);
    }

    @Override
    public int getL() {
        return L;
    }

    @Override
    public int getByteL() {
        return BYTE_L;
    }

    @Override
    public int getElementBitLength() {
        return L;
    }

    @Override
    public int getElementByteLength() {
        return BYTE_L;
    }

    @Override
    public byte[] createRandom(SecureRandom secureRandom) {
        byte[] element = new byte[BYTE_L];
        secureRandom.nextBytes(element);
        return element;
    }

    @Override
    public byte[] createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        return prg.extendToBytes(key);
    }

    @Override
    public byte[] createNonZeroRandom(SecureRandom secureRandom) {
        byte[] element = new byte[BYTE_L];
        while (isZero(element)) {
            secureRandom.nextBytes(element);
        }
        return element;
    }

    @Override
    public byte[] createNonZeroRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] element = prg.extendToBytes(key);
        while (isZero(element)) {
            key = kdf.deriveKey(key);
            element = prg.extendToBytes(key);
        }
        return element;
    }

    @Override
    public byte[] createRangeRandom(SecureRandom secureRandom) {
        return createRandom(secureRandom);
    }

    @Override
    public byte[] createRangeRandom(byte[] seed) {
        return createRandom(seed);
    }

    @Override
    public boolean isZero(byte[] p) {
        validateElement(p);
        return BytesUtils.equals(p, zero);
    }

    @Override
    public boolean isOne(byte[] p) {
        validateElement(p);
        return BytesUtils.equals(p, one);
    }

    @Override
    public boolean validateElement(byte[] p) {
        return p.length == BYTE_L;
    }

    @Override
    public boolean validateNonZeroElement(byte[] p) {
        return !isZero(p);
    }

    @Override
    public boolean validateRangeElement(byte[] p) {
        return validateElement(p);
    }
}