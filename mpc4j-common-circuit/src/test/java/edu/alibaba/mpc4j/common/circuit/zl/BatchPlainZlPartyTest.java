package edu.alibaba.mpc4j.common.circuit.zl;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.vector.ZlVector;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * batch plain Zl party test.
 *
 * @author Weiran Liu
 * @date 2023/5/9
 */
@RunWith(Parameterized.class)
public class BatchPlainZlPartyTest {
    /**
     * random status
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default num
     */
    private static final int DEFAULT_NUM = 1000;
    /**
     * large num
     */
    private static final int LARGE_NUM = 1 << 16;
    /**
     * vector length
     */
    private static final int VECTOR_LENGTH = 13;

    /**
     * l array
     */
    private static final int[] L_ARRAY = new int[]{
        1, 5, 7, 9, 15, 16, 17, LongUtils.MAX_L - 1, LongUtils.MAX_L, Long.SIZE, CommonConstants.BLOCK_BIT_LENGTH,
    };
    /**
     * Zl array
     */
    private static final Zl[] ZL_ARRAY = Arrays.stream(L_ARRAY)
        .mapToObj(l -> ZlFactory.createInstance(EnvType.STANDARD, l))
        .toArray(Zl[]::new);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        for (Zl zl : ZL_ARRAY) {
            configurations.add(new Object[]{"l = " + zl.getL(), zl});
        }

        return configurations;
    }

    /**
     * Zl instance
     */
    private final Zl zl;

    public BatchPlainZlPartyTest(String name, Zl zl) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.zl = zl;
    }

    @Test
    public void test1Num() {
        testPto(1);
    }

    @Test
    public void test2Num() {
        testPto(2);
    }

    @Test
    public void test8Num() {
        testPto(8);
    }

    @Test
    public void test15Num() {
        testPto(15);
    }

    @Test
    public void testDefaultNum() {
        testPto(DEFAULT_NUM);
    }

    @Test
    public void testLargeNum() {
        testPto(LARGE_NUM);
    }

    private void testPto(int num) {
        for (DyadicAcOperator operator : DyadicAcOperator.values()) {
            testDyadicOperator(operator, num);
        }
        for (UnaryAcOperator operator : UnaryAcOperator.values()) {
            testUnaryOperator(operator, num);
        }
    }

    private void testDyadicOperator(DyadicAcOperator operator, int num) {
        // generate x
        ZlVector[] xZlVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> ZlVector.createRandom(zl, num, SECURE_RANDOM))
            .toArray(ZlVector[]::new);
        PlainZlVector[] xPlainZlVectors = Arrays.stream(xZlVectors)
            .map(PlainZlVector::create)
            .toArray(PlainZlVector[]::new);
        // generate y
        ZlVector[] yZlVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> ZlVector.createRandom(zl, num, SECURE_RANDOM))
            .toArray(ZlVector[]::new);
        PlainZlVector[] yPlainZlVectors = Arrays.stream(yZlVectors)
            .map(PlainZlVector::create)
            .toArray(PlainZlVector[]::new);
        // create z
        ZlVector[] zZlVectors;
        PlainZlVector[] zPlainZlVectors;
        // operation
        PlainZlParty plainZlParty = new PlainZlParty(zl);
        plainZlParty.init(num * VECTOR_LENGTH, num * VECTOR_LENGTH);
        switch (operator) {
            case ADD:
                zZlVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(index -> xZlVectors[index].add(yZlVectors[index]))
                    .toArray(ZlVector[]::new);
                zPlainZlVectors = plainZlParty.add(xPlainZlVectors, yPlainZlVectors);
                break;
            case SUB:
                zZlVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(index -> xZlVectors[index].sub(yZlVectors[index]))
                    .toArray(ZlVector[]::new);
                zPlainZlVectors = plainZlParty.sub(xPlainZlVectors, yPlainZlVectors);
                break;
            case MUL:
                zZlVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(index -> xZlVectors[index].mul(yZlVectors[index]))
                    .toArray(ZlVector[]::new);
                zPlainZlVectors = plainZlParty.mul(xPlainZlVectors, yPlainZlVectors);
                break;
            default:
                throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        IntStream.range(0, VECTOR_LENGTH).forEach(index ->
            Assert.assertEquals(zZlVectors[index], zPlainZlVectors[index].getZlVector())
        );
    }

    @SuppressWarnings("SameParameterValue")
    private void testUnaryOperator(UnaryAcOperator operator, int num) {
        // generate x
        ZlVector[] xZlVectors = IntStream.range(0, VECTOR_LENGTH)
            .mapToObj(index -> ZlVector.createRandom(zl, num, SECURE_RANDOM))
            .toArray(ZlVector[]::new);
        PlainZlVector[] xPlainZlVectors = Arrays.stream(xZlVectors)
            .map(PlainZlVector::create)
            .toArray(PlainZlVector[]::new);
        // create z
        ZlVector[] zZlVectors;
        PlainZlVector[] zPlainZlVectors;
        // operation
        PlainZlParty plainZlParty = new PlainZlParty(zl);
        plainZlParty.init(num * VECTOR_LENGTH, num * VECTOR_LENGTH);
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NEG:
                zZlVectors = IntStream.range(0, VECTOR_LENGTH)
                    .mapToObj(index -> xZlVectors[index].neg())
                    .toArray(ZlVector[]::new);
                zPlainZlVectors = plainZlParty.neg(xPlainZlVectors);
                break;
            default:
                throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        IntStream.range(0, VECTOR_LENGTH).forEach(index ->
            Assert.assertEquals(zZlVectors[index], zPlainZlVectors[index].getZlVector())
        );
    }
}
