package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.galoisfield.BytesField;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * GF(2^l) interface.
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public interface Gf2e extends BytesField {
    /**
     * 返回运算类型。
     *
     * @return 运算类型。
     */
    Gf2eType getGf2eType();

    /**
     * Gets the bit length that represents an element. In GF(2^l), the element bit length is exactly l.
     *
     * @return the bit length that represents an element.
     */
    @Override
    default int getElementBitLength() {
        return getL();
    }

    /**
     * Gets the element byte length that represents an element. In GF(2^l), the element byte length is exactly l (in byte length).
     *
     * @return the element byte length that represents an element.
     */
    @Override
    default int getElementByteLength() {
        return getByteL();
    }

    /**
     * Computes p + q. In GF(2^l), p + q is bit-wise p ⊕ q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p + q.
     */
    @Override
    default byte[] add(final byte[] p, final byte[] q) {
        assert validateElement(p) && validateElement(q);
        return BytesUtils.xor(p, q);
    }

    /**
     * Computes p + q. The result is in-placed in p. In GF(2^l), p + q is bit-wise p ⊕ q.
     *
     * @param p the element p.
     * @param q the element q.
     */
    @Override
    default void addi(byte[] p, final byte[] q) {
        assert validateElement(p) && validateElement(q);
        BytesUtils.xori(p, q);
    }

    /**
     * Computes -p. In GF(2^l), -p is p.
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
     * Computes -p. The result is in-placed in p. In GF(2^l), -p is p.
     *
     * @param p the element p.
     */
    @Override
    default void negi(byte[] p) {
        assert validateElement(p);
    }

    /**
     * Computes p - q. In GF(2^l), p - q is p + (-q) = p + q.
     *
     * @param p the element p.
     * @param q the element q.
     * @return p - q.
     */
    @Override
    default byte[] sub(final byte[] p, final byte[] q) {
        // GF(2^l)元素的负数就是其本身，减法等价于加法
        return add(p, q);
    }

    /**
     * Computes p - q. The result is in-placed in p. In GF(2^l), p - q is p + (-q) = p + q.
     *
     * @param p the element p.
     * @param q the element q.
     */
    @Override
    default void subi(byte[] p, final byte[] q) {
        addi(p, q);
    }
}
