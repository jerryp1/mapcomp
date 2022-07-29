package edu.alibaba.mpc4j.common.tool.galoisfield.zp64;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.Zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.Zp64.Zp64Factory;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Zp64功能测试。
 *
 * @author Weiran Liu
 * @date 2022/7/7
 */
@RunWith(Parameterized.class)
public class Zp64Test {
    /**
     * 并发数量
     */
    private static final int MAX_PARALLEL = 10;
    /**
     * 随机测试数量
     */
    private static final int MAX_RANDOM_NUM = 40;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // Rings
        configurations.add(new Object[]{Zp64Factory.Zp64Type.RINGS.name() + " (l = 1)", Zp64Factory.Zp64Type.RINGS, 1});
        configurations.add(new Object[]{Zp64Factory.Zp64Type.RINGS.name() + " (l = 2)", Zp64Factory.Zp64Type.RINGS, 2});
        configurations.add(new Object[]{Zp64Factory.Zp64Type.RINGS.name() + " (l = 3)", Zp64Factory.Zp64Type.RINGS, 3});
        configurations.add(new Object[]{Zp64Factory.Zp64Type.RINGS.name() + " (l = 4)", Zp64Factory.Zp64Type.RINGS, 4});
        configurations.add(new Object[]{Zp64Factory.Zp64Type.RINGS.name() + " (l = 39)", Zp64Factory.Zp64Type.RINGS, 39});
        configurations.add(new Object[]{Zp64Factory.Zp64Type.RINGS.name() + " (l = 40)", Zp64Factory.Zp64Type.RINGS, 40});
        configurations.add(new Object[]{Zp64Factory.Zp64Type.RINGS.name() + " (l = 41)", Zp64Factory.Zp64Type.RINGS, 41});
        configurations.add(new Object[]{Zp64Factory.Zp64Type.RINGS.name() + " (l = 61)", Zp64Factory.Zp64Type.RINGS, 61});
        configurations.add(new Object[]{Zp64Factory.Zp64Type.RINGS.name() + " (l = 62)", Zp64Factory.Zp64Type.RINGS, 62});

        return configurations;
    }

    /**
     * GF(2^l)运算类型
     */
    private final Zp64Factory.Zp64Type type;
    /**
     * GF(2^l)运算
     */
    private final Zp64 zp64;
    /**
     * 用于测试并发性能的常数
     */
    private final long constant;

    public Zp64Test(String name, Zp64Factory.Zp64Type type, int l) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        zp64 = Zp64Factory.createInstance(EnvType.STANDARD, type, l);
        constant = 1L << l;
    }

    @Test
    public void testType() {
        Assert.assertEquals(type, zp64.getZp64Type());
    }

    @Test
    public void testModulus() {
        // 0 mod p = 0
        Assert.assertEquals(0L, zp64.module(0L));
        // 1 mod p = 1
        Assert.assertEquals(1L, zp64.module(1L));
        // p + 0 mod p = 0
        Assert.assertEquals(0L, zp64.module(zp64.getPrime()));
        // p + 1 mod p = 1
        Assert.assertEquals(1L, zp64.module(zp64.getPrime() + 1));
        // -1 mod p = p - 1
        Assert.assertEquals(zp64.getPrime() - 1, zp64.module(-1L));
        // 1 - p mod p = 1
        Assert.assertEquals(1L, zp64.module(1L - zp64.getPrime()));
    }

    @Test
    public void testAdd() {
        // 0 + 0 = 0
        long zero = zp64.createZero();
        Assert.assertEquals(zero, zp64.add(zero, zero));
        // 0 - 0 = 0
        Assert.assertEquals(zero, zp64.sub(zero, zero));
        // 0 + (-0) = 0
        Assert.assertEquals(zero, zp64.add(zero, zp64.neg(zero)));

        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            long a = zp64.randomNonZeroElement(SECURE_RANDOM);
            // a + (-a) = 0
            Assert.assertEquals(zero, zp64.add(a, zp64.neg(a)));
            // a - a = 0
            Assert.assertEquals(zero, zp64.sub(a, a));
        }
    }

    @Test
    public void testMul() {
        // 0 * 0 = 0
        long zero = zp64.createZero();
        Assert.assertEquals(zero, zp64.mul(zero, zero));
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            long a = zp64.randomNonZeroElement(SECURE_RANDOM);
            // 0 * a = 0
            Assert.assertEquals(zero, zp64.mul(zero, a));
            // a * 0 = 0
            Assert.assertEquals(zero, zp64.mul(a, zero));
        }
        // 1 * 1 = 1
        long one = zp64.createOne();
        Assert.assertEquals(one, zp64.mul(one, one));
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            long a = zp64.randomNonZeroElement(SECURE_RANDOM);
            // 1 * a = a
            Assert.assertEquals(a, zp64.mul(one, a));
            // a * 1 = a
            Assert.assertEquals(a, zp64.mul(a, one));
            // a * (1 / a) = 1
            Assert.assertEquals(one, zp64.mul(a, zp64.inv(a)));
            // a / a = 1
            Assert.assertEquals(one, zp64.div(a, a));
        }
    }

    @Test
    public void testModPow() {
        // 0^0 = 1
        long zero = zp64.createZero();
        long one = zp64.createOne();
        Assert.assertEquals(one, zp64.mulPow(zero, zero));
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            // 0^a = 0
            long a = zp64.randomNonZeroElement(SECURE_RANDOM);
            Assert.assertEquals(zero, zp64.mulPow(zero, a));
        }
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            // a^0 = 1
            long a = zp64.randomNonZeroElement(SECURE_RANDOM);
            Assert.assertEquals(one, zp64.mulPow(a, zero));
        }
        for (int round = 0; round < MAX_RANDOM_NUM; round++) {
            // (a^b)^(-1) = (a^(-1))^b
            long a = zp64.randomNonZeroElement(SECURE_RANDOM);
            long b = zp64.randomNonZeroElement(SECURE_RANDOM);
            Assert.assertEquals(zp64.inv(zp64.mulPow(a, b)), zp64.mulPow(zp64.inv(a), b));
        }
    }

    @Test
    public void testAddParallel() {
        long addCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> zp64.add(constant, constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, addCount);

        long subCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> zp64.sub(constant, constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, subCount);

        long negCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> zp64.neg(constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, negCount);
    }

    @Test
    public void testMulParallel() {
        long mulCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> zp64.mul(constant, constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, mulCount);

        long divCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> zp64.div(constant, constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, divCount);

        long invCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> zp64.inv(constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, invCount);
    }

    @Test
    public void testModPowParallel() {
        long mulPowCount = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToLong(index -> zp64.mulPow(constant, constant))
            .distinct()
            .count();
        Assert.assertEquals(1L, mulPowCount);
    }
}
