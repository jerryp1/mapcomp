package edu.alibaba.mpc4j.common.tool.galoisfield;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * BytesRing tests.
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
@RunWith(Parameterized.class)
public class BytesRingTest {
    /**
     * parallel num
     */
    private static final int MAX_PARALLEL = 10;
    /**
     * random test num
     */
    private static final int MAX_RANDOM = 400;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();



        // GF2K (SSE)
        configurationParams.add(new Object[]{
            "GF2K(" + Gf2kFactory.Gf2kType.SSE.name() + ")",
            Gf2kFactory.createInstance(Gf2kFactory.Gf2kType.SSE, EnvType.STANDARD),
        });
        // GF2K (NTL)
        configurationParams.add(new Object[]{
            "GF2K(" + Gf2kFactory.Gf2kType.NTL.name() + ")",
            Gf2kFactory.createInstance(Gf2kFactory.Gf2kType.NTL, EnvType.STANDARD),
        });
        // GF2K (BC)
        configurationParams.add(new Object[]{
            "GF2K(" + Gf2kFactory.Gf2kType.BC.name() + ")",
            Gf2kFactory.createInstance(Gf2kFactory.Gf2kType.BC, EnvType.STANDARD),
        });
        // RINGS
        configurationParams.add(new Object[]{
            "GF2K(" + Gf2kFactory.Gf2kType.RINGS.name() + ")",
            Gf2kFactory.createInstance(Gf2kFactory.Gf2kType.RINGS, EnvType.STANDARD),
        });

        return configurationParams;
    }

    /**
     * the BytesRings instance
     */
    private final BytesRing bytesRing;
    /**
     * the l byte length
     */
    private final int elementByteLength;

    public BytesRingTest(String name, BytesRing bytesRing) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.bytesRing = bytesRing;
        elementByteLength = bytesRing.getElementByteLength();
    }

    @Test
    public void testIllegalInputs() {
        // try operating p and q when p has invalid byte length
        final byte[] invalidP = new byte[elementByteLength - 1];
        final byte[] q = new byte[elementByteLength];
        // try adding
        Assert.assertThrows(AssertionError.class, () -> bytesRing.add(invalidP, q));
        Assert.assertThrows(AssertionError.class, () -> bytesRing.addi(invalidP, q));
        // try multiplying
        Assert.assertThrows(AssertionError.class, () -> bytesRing.mul(invalidP, q));
        Assert.assertThrows(AssertionError.class, () -> bytesRing.muli(invalidP, q));

        // try operating p and q when q has invalid byte length
        final byte[] p = new byte[elementByteLength];
        final byte[] invalidQ = new byte[elementByteLength - 1];
        // try adding
        Assert.assertThrows(AssertionError.class, () -> bytesRing.add(p, invalidQ));
        Assert.assertThrows(AssertionError.class, () -> bytesRing.addi(p, invalidQ));
        // try multiplying
        Assert.assertThrows(AssertionError.class, () -> bytesRing.mul(p, invalidQ));
        Assert.assertThrows(AssertionError.class, () -> bytesRing.muli(p, invalidQ));
    }

    @Test
    public void testRandomAddSub() {
        byte[] zero = bytesRing.createZero();
        for (int index = 0; index < MAX_RANDOM; index++) {
            byte[] r = bytesRing.createRandom(SECURE_RANDOM);
            byte[] copyR;
            byte[] s = bytesRing.createRandom(SECURE_RANDOM);
            // r + 0 = r
            Assert.assertArrayEquals(r, bytesRing.add(r, zero));
            copyR = BytesUtils.clone(r);
            bytesRing.addi(copyR, zero);
            Assert.assertArrayEquals(r, copyR);
            // r - 0 = r
            Assert.assertArrayEquals(r, bytesRing.sub(r, zero));
            copyR = BytesUtils.clone(r);
            bytesRing.subi(copyR, zero);
            Assert.assertArrayEquals(r, copyR);
            // -(-r) = r
            Assert.assertArrayEquals(r, bytesRing.neg(bytesRing.neg(r)));
            copyR = BytesUtils.clone(r);
            bytesRing.negi(copyR);
            bytesRing.negi(copyR);
            Assert.assertArrayEquals(r, copyR);
            // r + s - s = r
            Assert.assertArrayEquals(r, bytesRing.sub(bytesRing.add(r, s), s));
            copyR = BytesUtils.clone(r);
            bytesRing.addi(copyR, s);
            bytesRing.subi(copyR, s);
            Assert.assertArrayEquals(r, copyR);
            // r - s + s = r
            Assert.assertArrayEquals(r, bytesRing.add(bytesRing.sub(r, s), s));
            copyR = BytesUtils.clone(r);
            bytesRing.subi(copyR, s);
            bytesRing.addi(copyR, s);
            Assert.assertArrayEquals(r, copyR);
        }
    }

    @Test
    public void testConstantMultiply() {
        byte[] zero = bytesRing.createZero();
        byte[] one = bytesRing.createOne();
        byte[] p;
        byte[] copyP;
        byte[] t;
        // 0 * 0 = 0
        p = bytesRing.createZero();
        t = bytesRing.mul(p, zero);
        Assert.assertArrayEquals(zero, t);
        copyP = BytesUtils.clone(p);
        bytesRing.muli(copyP, zero);
        Assert.assertArrayEquals(zero, copyP);
        // 1 * 1 = 1
        p = bytesRing.createOne();
        t = bytesRing.mul(p, one);
        Assert.assertArrayEquals(one, t);
        copyP = BytesUtils.clone(p);
        bytesRing.muli(copyP, one);
        Assert.assertArrayEquals(one, copyP);
        copyP = BytesUtils.clone(p);
        bytesRing.muli(copyP, copyP);
        Assert.assertArrayEquals(one, copyP);
    }

    @Test
    public void testRandomMultiply() {
        byte[] zero = bytesRing.createZero();
        byte[] one = bytesRing.createOne();
        byte[] r;
        byte[] copyR;
        byte[] t;
        for (int index = 0; index < MAX_RANDOM; index++) {
            // r * 0 = 0
            r = bytesRing.createRandom(SECURE_RANDOM);
            t = bytesRing.mul(r, zero);
            Assert.assertArrayEquals(zero, t);
            copyR = BytesUtils.clone(r);
            bytesRing.muli(copyR, zero);
            Assert.assertArrayEquals(zero, copyR);
            // r * 1 = r
            r = bytesRing.createNonZeroRandom(SECURE_RANDOM);
            t = bytesRing.mul(r, one);
            Assert.assertArrayEquals(r, t);
            copyR = BytesUtils.clone(r);
            bytesRing.muli(copyR, one);
            Assert.assertArrayEquals(r, copyR);
        }
    }

    @Test
    public void testParallel() {
        Set<ByteBuffer> cArray = IntStream.range(0, MAX_PARALLEL)
            .mapToObj(index -> {
                byte[] a = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                Arrays.fill(a, (byte) 0xFF);
                byte[] b = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                Arrays.fill(b, (byte) 0xFF);
                return bytesRing.mul(a, b);
            }).map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, cArray.size());

        Set<ByteBuffer> aArray = IntStream.range(0, MAX_PARALLEL)
            .mapToObj(index -> {
                byte[] a = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                Arrays.fill(a, (byte) 0xFF);
                byte[] b = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                Arrays.fill(b, (byte) 0xFF);
                bytesRing.muli(a, b);
                return a;
            }).map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, aArray.size());
    }
}
