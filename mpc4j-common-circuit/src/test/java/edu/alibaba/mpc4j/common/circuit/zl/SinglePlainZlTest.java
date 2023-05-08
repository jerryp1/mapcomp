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

/**
 * single plain Zl circuit test.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
@RunWith(Parameterized.class)
public class SinglePlainZlTest {
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

    public SinglePlainZlTest(String name, Zl zl) {
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
        testDyadicOperator(DyadicAcOperator.ADD, num);
        testDyadicOperator(DyadicAcOperator.SUB, num);
        testDyadicOperator(DyadicAcOperator.MUL, num);
        testUnaryOperator(UnaryAcOperator.NEG, num);
    }

    private void testDyadicOperator(DyadicAcOperator operator, int num) {
        // generate x
        ZlVector xZlVector = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        PlainZlVector xPlainZlVector = PlainZlVector.create(xZlVector);
        // generate y
        ZlVector yZlVector = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        PlainZlVector yPlainZlVector = PlainZlVector.create(yZlVector);
        // create z
        ZlVector zZlVector;
        PlainZlVector zPlainZlVector;
        // operation
        PlainZlParty plainZlParty = new PlainZlParty(zl);
        switch (operator) {
            case ADD:
                zZlVector = xZlVector.add(yZlVector);
                zPlainZlVector = plainZlParty.add(xPlainZlVector, yPlainZlVector);
                break;
            case SUB:
                zZlVector = xZlVector.sub(yZlVector);
                zPlainZlVector = plainZlParty.sub(xPlainZlVector, yPlainZlVector);
                break;
            case MUL:
                zZlVector = xZlVector.mul(yZlVector);
                zPlainZlVector = plainZlParty.mul(xPlainZlVector, yPlainZlVector);
                break;
            default:
                throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertEquals(zZlVector, zPlainZlVector.getZlVector());
    }

    @SuppressWarnings("SameParameterValue")
    private void testUnaryOperator(UnaryAcOperator operator, int num) {
        // generate x
        ZlVector xZlVector = ZlVector.createRandom(zl, num, SECURE_RANDOM);
        PlainZlVector xPlainZlVector = PlainZlVector.create(xZlVector);
        // create z
        ZlVector zZlVector;
        PlainZlVector zPlainZlVector;
        // operation
        PlainZlParty plainZlParty = new PlainZlParty(zl);
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NEG:
                zZlVector = xZlVector.neg();
                zPlainZlVector = plainZlParty.neg(xPlainZlVector);
                break;
            default:
                throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
        }
        // verify
        Assert.assertEquals(zZlVector, zPlainZlVector.getZlVector());
    }
}
