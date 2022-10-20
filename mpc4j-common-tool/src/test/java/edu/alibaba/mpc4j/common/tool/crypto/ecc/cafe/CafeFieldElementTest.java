/*
 * This file is part of curve25519-elisabeth.
 * Copyright (c) 2019 Jack Grigg
 * See LICENSE for licensing information.
 */

package edu.alibaba.mpc4j.common.tool.crypto.ecc.cafe;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test vectors below, and the tests they are used in, are from curve25519-dalek.
 * <p>
 * https://github.com/dalek-cryptography/curve25519-dalek/blob/4bdccd7b7c394d9f8ffc4b29d5acc23c972f3d7a/src/field.rs#L280-L301
 * </p>
 * Modified from:
 * <p>
 * https://github.com/cryptography-cafe/curve25519-elisabeth/blob/main/src/test/java/cafe/cryptography/curve25519/FieldElementTest.java
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/10/20
 */
public class CafeFieldElementTest {
    /**
     * Random element a of GF(2^255-19), from Sage.
     * a = 10703145068883540813293858232352184442332212228051251926706380353716438957572
     */
    private static final byte[] A_BYTES_COORD = {
        (byte) 0x04, (byte) 0xfe, (byte) 0xdf, (byte) 0x98, (byte) 0xa7, (byte) 0xfa, (byte) 0x0a, (byte) 0x68,
        (byte) 0x84, (byte) 0x92, (byte) 0xbd, (byte) 0x59, (byte) 0x08, (byte) 0x07, (byte) 0xa7, (byte) 0x03,
        (byte) 0x9e, (byte) 0xd1, (byte) 0xf6, (byte) 0xf2, (byte) 0xe1, (byte) 0xd9, (byte) 0xe2, (byte) 0xa4,
        (byte) 0xa4, (byte) 0x51, (byte) 0x47, (byte) 0x36, (byte) 0xf3, (byte) 0xc3, (byte) 0xa9, (byte) 0x17,
    };
    /**
     * Byte representation of a^2
     */
    private static final byte[] A_SQUARE_BYTES_COORD = {
        (byte) 0x75, (byte) 0x97, (byte) 0x24, (byte) 0x9e, (byte) 0xe6, (byte) 0x06, (byte) 0xfe, (byte) 0xab,
        (byte) 0x24, (byte) 0x04, (byte) 0x56, (byte) 0x68, (byte) 0x07, (byte) 0x91, (byte) 0x2d, (byte) 0x5d,
        (byte) 0x0b, (byte) 0x0f, (byte) 0x3f, (byte) 0x1c, (byte) 0xb2, (byte) 0x6e, (byte) 0xf2, (byte) 0xe2,
        (byte) 0x63, (byte) 0x9c, (byte) 0x12, (byte) 0xba, (byte) 0x73, (byte) 0x0b, (byte) 0xe3, (byte) 0x62,
    };
    /**
     * Byte representation of 1/a
     */
    private static final byte[] A_INVERSE_BYTES_COORD = {
        (byte) 0x96, (byte) 0x1b, (byte) 0xcd, (byte) 0x8d, (byte) 0x4d, (byte) 0x5e, (byte) 0xa2, (byte) 0x3a,
        (byte) 0xe9, (byte) 0x36, (byte) 0x37, (byte) 0x93, (byte) 0xdb, (byte) 0x7b, (byte) 0x4d, (byte) 0x70,
        (byte) 0xb8, (byte) 0x0d, (byte) 0xc0, (byte) 0x55, (byte) 0xd0, (byte) 0x4c, (byte) 0x1d, (byte) 0x7b,
        (byte) 0x90, (byte) 0x71, (byte) 0xd8, (byte) 0xe9, (byte) 0xb6, (byte) 0x18, (byte) 0xe6, (byte) 0x30,
    };
    /**
     * Byte representation of a^((p-5)/8)
     */
    private static final byte[] A_P58_BYTES_COORD = {
        (byte) 0x6a, (byte) 0x4f, (byte) 0x24, (byte) 0x89, (byte) 0x1f, (byte) 0x57, (byte) 0x60, (byte) 0x36,
        (byte) 0xd0, (byte) 0xbe, (byte) 0x12, (byte) 0x3c, (byte) 0x8f, (byte) 0xf5, (byte) 0xb1, (byte) 0x59,
        (byte) 0xe0, (byte) 0xf0, (byte) 0xb8, (byte) 0x1b, (byte) 0x20, (byte) 0xd2, (byte) 0xb5, (byte) 0x1f,
        (byte) 0x15, (byte) 0x21, (byte) 0xf9, (byte) 0xe3, (byte) 0xe1, (byte) 0x61, (byte) 0x21, (byte) 0x55,
    };

    @Test
    public void testMultiply() {
        final int[] a = CafeFieldElement.decode(A_BYTES_COORD);
        final int[] aSquare = CafeFieldElement.decode(A_SQUARE_BYTES_COORD);
        Assert.assertTrue(CafeFieldElement.equals(CafeFieldElement.multiply(a, a), aSquare));
    }

    @Test
    public void testSquare() {
        final int[] a = CafeFieldElement.decode(A_BYTES_COORD);
        final int[] aSquare = CafeFieldElement.decode(A_SQUARE_BYTES_COORD);
        Assert.assertTrue(CafeFieldElement.equals(CafeFieldElement.square(a), aSquare));
    }

    @Test
    public void testSquareDouble() {
        final int[] a = CafeFieldElement.decode(A_BYTES_COORD);
        final int[] aSquare = CafeFieldElement.decode(A_SQUARE_BYTES_COORD);
        Assert.assertTrue(CafeFieldElement.equals(
            CafeFieldElement.squareAndDouble(a), CafeFieldElement.add(aSquare, aSquare)
        ));
    }

    @Test
    public void testInvert() {
        final int[] a = CafeFieldElement.decode(A_BYTES_COORD);
        final int[] aInverseConstant = CafeFieldElement.decode(A_INVERSE_BYTES_COORD);
        final int[] aInverse = CafeFieldElement.invert(a);
        Assert.assertTrue(CafeFieldElement.equals(aInverse, aInverseConstant));
        Assert.assertTrue(CafeFieldElement.equals(
            CafeFieldElement.multiply(a, aInverse), CafeFieldElement.ONE_INTS_COORD
        ));
    }

    @Test
    public void testSqrtRatioM1() {
        int[] zero = CafeFieldElement.ZERO_INTS_COORD;
        int[] one = CafeFieldElement.ONE_INTS_COORD;
        int[] i = CafeConstants.SQRT_M1;
        // 2 is non-square mod p.
        int[] two = CafeFieldElement.add(one, one);
        // 4 is square mod p.
        int[] four = CafeFieldElement.add(two, two);
        CafeFieldElement.SqrtRatioM1Result sqrt;

        // 0/0 should return (1, 0) since u is 0
        sqrt = CafeFieldElement.sqrtRatioM1(zero, zero);
        Assert.assertEquals(sqrt.wasSquare, 1);
        Assert.assertTrue(CafeFieldElement.equals(sqrt.result, zero));
        Assert.assertEquals(CafeFieldElement.isNegative(sqrt.result), 0);

        // 1/0 should return (0, 0) since v is 0, u is nonzero
        sqrt = CafeFieldElement.sqrtRatioM1(one, zero);
        Assert.assertEquals(sqrt.wasSquare, 0);
        Assert.assertTrue(CafeFieldElement.equals(sqrt.result, zero));
        Assert.assertEquals(CafeFieldElement.isNegative(sqrt.result), 0);

        // 2/1 is non-square, so we expect (0, sqrt(i*2))
        sqrt = CafeFieldElement.sqrtRatioM1(two, one);
        Assert.assertEquals(sqrt.wasSquare, 0);
        Assert.assertTrue(CafeFieldElement.equals(
            CafeFieldElement.square(sqrt.result), CafeFieldElement.multiply(two, i)
        ));
        Assert.assertEquals(CafeFieldElement.isNegative(sqrt.result), 0);

        // 4/1 is square, so we expect (1, sqrt(4))
        sqrt = CafeFieldElement.sqrtRatioM1(four, one);
        Assert.assertEquals(sqrt.wasSquare, 1);
        Assert.assertTrue(CafeFieldElement.equals(CafeFieldElement.square(sqrt.result), four));
        Assert.assertEquals(CafeFieldElement.isNegative(sqrt.result), 0);

        // 1/4 is square, so we expect (1, 1/sqrt(4))
        sqrt = CafeFieldElement.sqrtRatioM1(one, four);
        Assert.assertEquals(sqrt.wasSquare, 1);
        Assert.assertTrue(CafeFieldElement.equals(
            CafeFieldElement.multiply(CafeFieldElement.square(sqrt.result), four), one
        ));
        Assert.assertEquals(CafeFieldElement.isNegative(sqrt.result), 0);
    }

    @Test
    public void testP58() {
        int[] a = CafeFieldElement.decode(A_BYTES_COORD);
        int[] ap58 = CafeFieldElement.decode(A_P58_BYTES_COORD);
        Assert.assertTrue(CafeFieldElement.equals(CafeFieldElement.powP58(a), ap58));
    }

    @Test
    public void testEquals() {
        final int[] a = CafeFieldElement.decode(A_BYTES_COORD);
        final int[] aInverse = CafeFieldElement.decode(A_INVERSE_BYTES_COORD);
        Assert.assertTrue(CafeFieldElement.equals(a, a));
        Assert.assertFalse(CafeFieldElement.equals(a, aInverse));
    }

    /**
     * Notice that the last element has the high bit set, which should be ignored.
     */
    private static final byte[] B_BYTES_COORD = {
        (byte) 0x71, (byte) 0xbf, (byte) 0xa9, (byte) 0x8f, (byte) 0x5b, (byte) 0xea, (byte) 0x79, (byte) 0x0f,
        (byte) 0xf1, (byte) 0x83, (byte) 0xd9, (byte) 0x24, (byte) 0xe6, (byte) 0x65, (byte) 0x5c, (byte) 0xea,
        (byte) 0x08, (byte) 0xd0, (byte) 0xaa, (byte) 0xfb, (byte) 0x61, (byte) 0x7f, (byte) 0x46, (byte) 0xd2,
        (byte) 0x3a, (byte) 0x17, (byte) 0xa6, (byte) 0x57, (byte) 0xf0, (byte) 0xa9, (byte) 0xb8, (byte) 0xb2,
    };

    @Test
    public void testDecodeHighBitIgnored() {
        byte[] clearedBytes = BytesUtils.clone(B_BYTES_COORD);
        clearedBytes[CafeFieldElement.BYTES_COORD_SIZE - 1] &= 0x7f;
        int[] withHighBitSet = CafeFieldElement.decode(B_BYTES_COORD);
        int[] withoutHighBitSet = CafeFieldElement.decode(clearedBytes);
        Assert.assertTrue(CafeFieldElement.equals(withHighBitSet, withoutHighBitSet));
    }

    /**
     * Encode 1 wrongly as 1 + (2^255 - 19) = 2^255 - 18
     */
    private static final byte[] ONE_CANONICAL_BYTES_COORD = {
        (byte) 0xee, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x7f,
    };

    @Test
    public void testEncodeCanonical() {
        // Decode to a field element
        int[] one = CafeFieldElement.decode(ONE_CANONICAL_BYTES_COORD);
        // Then check that the encoding is correct
        Assert.assertArrayEquals(one, CafeFieldElement.ONE_INTS_COORD);
        Assert.assertTrue(CafeFieldElement.equals(CafeFieldElement.ONE_INTS_COORD, one));
    }

    @Test
    public void testEncodeZero() {
        byte[] zero = new byte[CafeFieldElement.BYTES_COORD_SIZE];
        final int[] a = CafeFieldElement.decode(zero);
        Assert.assertArrayEquals(a, CafeFieldElement.ZERO_INTS_COORD);
        Assert.assertTrue(CafeFieldElement.equals(CafeFieldElement.ZERO_INTS_COORD, a));
    }

    @Test
    public void testConstantTimeSelect() {
        int[] a = new int[CafeFieldElement.INTS_COORD_SIZE];
        int[] b = new int[CafeFieldElement.INTS_COORD_SIZE];
        for (int i = 0; i < CafeFieldElement.INTS_COORD_SIZE; i++) {
            a[i] = i;
            b[i] = CafeFieldElement.INTS_COORD_SIZE - i;
        }
        Assert.assertArrayEquals(CafeFieldElement.constantTimeSelect(a, b, 0), a);
        Assert.assertArrayEquals(CafeFieldElement.constantTimeSelect(a, b, 1), b);
        Assert.assertArrayEquals(CafeFieldElement.constantTimeSelect(b, a, 0), b);
        Assert.assertArrayEquals(CafeFieldElement.constantTimeSelect(b, a, 1), a);
    }
}
