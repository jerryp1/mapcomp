package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.BytesRing;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory.Gf2kType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * GF(2^128)有限域运算接口。
 *
 * @author Weiran Liu
 * @date 2022/01/15
 */
public interface Gf2k extends BytesRing {
    /**
     * Gets GF(2^λ) type.
     *
     * @return GF(2^λ) type.
     */
    Gf2kType getGf2kType();

    /**
     * Computes p + q. In GF(2^λ), p + q is bit-wise p ⊕ q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p + q.
     */
    @Override
    default byte[] add(final byte[] p, final byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        return BytesUtils.xor(p, q);
    }

    /**
     * Computes p + q. The result is in-placed in p. In GF(2^λ), p + q is bit-wise p ⊕ q.
     *
     * @param p the element p.
     * @param q the element q.
     */
    @Override
    default void addi(byte[] p, final byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        BytesUtils.xori(p, q);
    }

    /**
     * Computes -p. GF(2^λ), -p is p.
     *
     * @param p the element p.
     * @return -p.
     */
    @Override
    default byte[] neg(byte[] p) {
        assert validateElement(p);
        return BytesUtils.clone(p);
    }

    /**
     * Computes -p. The result is in-placed in p. GF(2^λ), -p is p.
     *
     * @param p the element p.
     */
    @Override
    default void negi(byte[] p) {
        assert validateElement(p);
    }

    /**
     * Computes p - q. In GF(2^λ), p - q = p + (-q) = p + q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p - q.
     */
    @Override
    default byte[] sub(final byte[] p, final byte[] q) {
        return add(p, q);
    }

    /**
     * Computes p - q. The result is in-placed in p. In GF(2^λ), p - q = p + (-q) = p + q.
     *
     * @param p the element p.
     * @param q the element q.
     */
    @Override
    default void subi(byte[] p, final byte[] q) {
        addi(p, q);
    }

    /**
     * Creates a zero element.
     *
     * @return a zero element.
     */
    @Override
    default byte[] createZero() {
        return new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    }

    /**
     * Creates an identity element.
     *
     * @return an identity element.
     */
    @Override
    default byte[] createOne() {
        return new byte[]{
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01
        };
    }
}
