package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory.ByteEccType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 字节椭圆曲线测试。
 *
 * @author Weiran Liu
 * @date 2022/9/1
 */
@RunWith(Parameterized.class)
public class ByteEccTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 100;
    /**
     * 并发数量
     */
    private static final int PARALLEL_NUM = 400;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();

        // ED25519_BC
        configurationParams.add(new Object[]{ByteEccType.ED25519_BC.name(), ByteEccType.ED25519_BC,});

        return configurationParams;
    }

    /**
     * 待测试的字节椭圆曲线类型
     */
    private final ByteEccType byteEccType;

    public ByteEccTest(String name, ByteEccType byteEccType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.byteEccType = byteEccType;
    }

    @Test
    public void testIllegalInputs() {
        ByteEcc byteEcc = ByteEccFactory.createInstance(byteEccType);
        // 尝试将长度为0的字节数组映射到椭圆曲线上
        try {
            byteEcc.hashToCurve(new byte[0]);
            throw new IllegalStateException("ERROR: successfully HashToCurve with 0-byte length message");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        ByteEcc byteEcc = ByteEccFactory.createInstance(byteEccType);
        Assert.assertEquals(byteEccType, byteEcc.getByteEccType());
    }

    @Test
    public void testHashToCurve() {
        testHashToCurve(1);
        testHashToCurve(CommonConstants.STATS_BYTE_LENGTH);
        testHashToCurve(CommonConstants.BLOCK_BYTE_LENGTH);
        testHashToCurve(2 * CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testHashToCurve(int messageByteLength) {
        ByteEcc byteEcc = ByteEccFactory.createInstance(byteEccType);
        byte[] message = new byte[messageByteLength];
        byte[] hash1 = byteEcc.hashToCurve(message);
        Assert.assertTrue(byteEcc.isValid(hash1));
        byte[] hash2 = byteEcc.hashToCurve(message);
        Assert.assertArrayEquals(hash1, hash2);
    }

    @Test
    public void testRandomHashToCurve() {
        ByteEcc byteEcc = ByteEccFactory.createInstance(byteEccType);
        Set<ByteBuffer> hashPointSet = new HashSet<>();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byte[] message = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(message);
            byte[] p = byteEcc.hashToCurve(message);
            Assert.assertTrue(byteEcc.isValid(p));
            hashPointSet.add(ByteBuffer.wrap(byteEcc.hashToCurve(message)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, hashPointSet.size());
    }

    @Test
    public void testMul() {
        ByteEcc byteEcc = ByteEccFactory.createInstance(byteEccType);
        byte[] g = byteEcc.getG();
        // 生成一个未归一化（normalized）的椭圆曲线点h
        byte[] h = byteEcc.mul(g, byteEcc.randomZn(SECURE_RANDOM));
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // 生成r和r^{-1}
            BigInteger r = byteEcc.randomZn(SECURE_RANDOM);
            BigInteger rInv = r.modInverse(byteEcc.getN());
            // g^r
            byte[] gr = byteEcc.mul(g, r);
            byte[] grInv = byteEcc.mul(gr, rInv);
            Assert.assertArrayEquals(g, grInv);
            // h^r
            byte[] hr = byteEcc.mul(h, r);
            byte[] hrInv = byteEcc.mul(hr, rInv);
            Assert.assertArrayEquals(h, hrInv);
        }
    }

    @Test
    public void testBaseMul() {
        ByteEcc byteEcc = ByteEccFactory.createInstance(byteEccType);
        byte[] g = byteEcc.getG();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // 生成r和r^{-1}
            BigInteger r = byteEcc.randomZn(SECURE_RANDOM);
            BigInteger rInv = r.modInverse(byteEcc.getN());
            // g^r
            byte[] gr = byteEcc.mul(g, r);
            // 应用底数幂乘计算
            byte[] baseGr = byteEcc.baseMul(r);
            Assert.assertArrayEquals(gr, baseGr);
            byte[] baseGrInv = byteEcc.mul(gr, rInv);
            Assert.assertArrayEquals(g, baseGrInv);
        }
    }

    @Test
    public void testAdd() {
        ByteEcc byteEcc = ByteEccFactory.createInstance(byteEccType);
        byte[] g = byteEcc.getG();
        byte[] expect = byteEcc.baseMul(BigInteger.valueOf(MAX_RANDOM_ROUND));
        // 连续求和
        byte[] actual = byteEcc.getInfinity();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            actual = byteEcc.add(actual, g);
        }
        Assert.assertArrayEquals(expect, actual);
        // 连续内部求和
        actual = byteEcc.getInfinity();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byteEcc.addi(actual, g);
        }
        Assert.assertArrayEquals(expect, actual);
        byte[] positive = BytesUtils.clone(actual);

        BigInteger n = byteEcc.getN();
        expect = byteEcc.baseMul(BigInteger.valueOf(MAX_RANDOM_ROUND).negate().mod(n));
        actual = byteEcc.getInfinity();
        // 连续求差
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            actual = byteEcc.sub(actual, g);
        }
        Assert.assertArrayEquals(expect, actual);
        // 连续内部求差
        actual = byteEcc.getInfinity();
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            byteEcc.subi(actual, g);
        }
        Assert.assertArrayEquals(expect, actual);
        byte[] negative = BytesUtils.clone(actual);

        // 验证求逆
        expect = byteEcc.neg(negative);
        Assert.assertArrayEquals(positive, expect);
        expect = byteEcc.neg(positive);
        Assert.assertArrayEquals(negative, expect);
        // 验证内部求逆
        expect = BytesUtils.clone(negative);
        byteEcc.negi(expect);
        Assert.assertArrayEquals(positive, expect);
        expect = BytesUtils.clone(positive);
        byteEcc.negi(expect);
        Assert.assertArrayEquals(negative, expect);
    }

    @Test
    public void testParallel() {
        ByteEcc byteEcc = ByteEccFactory.createInstance(byteEccType);
        // HashToCurve并发测试
        byte[][] messages = IntStream.range(0, PARALLEL_NUM)
            .mapToObj(index -> new byte[CommonConstants.BLOCK_BYTE_LENGTH])
            .toArray(byte[][]::new);
        Set<ByteBuffer> hashMessageSet = Arrays.stream(messages)
            .parallel()
            .map(byteEcc::hashToCurve)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashMessageSet.size());
        // RandomPoint并发测试
        Set<ByteBuffer> randomPointSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> byteEcc.randomPoint(SECURE_RANDOM))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(PARALLEL_NUM, randomPointSet.size());
        // 运算并发测试
        byte[] p = byteEcc.randomPoint(SECURE_RANDOM);
        byte[] q = byteEcc.randomPoint(SECURE_RANDOM);
        // 加法并发测试
        Set<ByteBuffer> addSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> byteEcc.add(p, q))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, addSet.size());
        // 减法并发测试
        Set<ByteBuffer> subSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> byteEcc.sub(p, q))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, subSet.size());
        // 乘法并发测试
        BigInteger r = BigIntegerUtils.randomPositive(byteEcc.getN(), SECURE_RANDOM);
        Set<ByteBuffer> mulSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> byteEcc.mul(p, r))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, mulSet.size());
    }
}
